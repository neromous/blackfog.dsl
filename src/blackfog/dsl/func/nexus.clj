(ns blackfog.dsl.func.nexus
  (:require [clojure.string :as str]
            [clojure.pprint :as pp]
            [clojure.spec.alpha :as s]
            [blackfog.dsl.core :refer [render]]
            [blackfog.llm.client :as client]
            [blackfog.spec :as spec]
            [clojure.core.async :as async :refer [go <! <!! >! chan]]
            [blackfog.dsl.extractor.core :refer [extract-content]]
            [blackfog.dsl.extractor.core :as extract])
  (:import [java.time LocalDateTime LocalDate ZonedDateTime ZoneId]
           [java.time.format DateTimeFormatter]
           [java.time.temporal ChronoUnit]))

;; messages
(defn user-msg [& content]
  {:role "user"
   :content (str/join "\n\n" content)})

(defn assistant-msg [& content]
  {:role "assistant"
   :content (str/join "\n\n" content)})

(defn system-msg [& content]
  {:role "system"
   :content (str/join "\n\n" content)})

(defn- coll->messages [coll]
  (reduce (fn [m n] (cond
                      (s/valid? ::spec/message n)
                      (conj m (select-keys n [:role :content :reasoning_content]))

                      (s/valid? ::spec/messages n)
                      (into m (map #(select-keys % [:role :content :reasoning_content]) n))

                      :else m))
          [] coll))

(defn messages [& coll] (coll->messages coll))

;; ================================
(defn msg->extracted [msg]
  (if (s/valid? ::spec/message msg)
    (let [content (get msg :content)
          reasoning_in_msg (get msg :reasoning_content)
          extracted (extract-content content)]
      (merge extracted
             {:success true
              :raw content
              :content (or (:content/visible extracted) content)
              :think   (or reasoning_in_msg  (:content/think extracted))}))

    {:success false
     :error "Invalid message format"}))

(defn response-extractor [msg]
  (cond
    (s/valid? ::spec/message msg)
    (msg->extracted msg)

    (s/valid? ::spec/messages msg)
    (into [] (map msg->extracted msg))

    :else []))

;;
(defn content-from-extracted [extracted]
  (let [text (:content extracted)]
    text))

;; ================================
(defn sync-request
  "等待异步响应通道返回结果，支持超时处理
   
   Parameters:
   - config 配置参数映射，包含 :receiver 和 :timeout-ms 等选项
     - :receiver 指定接收者，默认为 :default
     - :timeout-ms 超时时间（毫秒），默认为 30000
     - :extractor-fn 提取函数，默认为 :default, 可选值为 :data, :handler
   - msgs 要发送的消息列表
   
   Returns:
   - {:success true :data <response-data>} 成功时
   - {:success false :error <error-message>} 失败或超时时"
  [{:keys [receiver timeout-ms extractor validator callback postfix]
    :or {receiver :default
         postfix identity
         validator identity
         timeout-ms 30000
         extractor :content}} msgs]
  {:pre [(or (every? map? msgs)
             (map? msgs))
         (or (keyword? extractor)
             (fn? extractor))]}
  (let [msgs (if (map? msgs) [msgs] msgs)
        receiver (or receiver :default)
        response-chan (client/async-post receiver "/chat/completions" {:messages msgs})
        extractor-fn (case extractor
                       :raw  identity
                       :content :content 
                       :think :reasoning_content
                       :default msg->extracted
                       :with-think (fn [msg]
                                     (if-let [think (:reasoning_content msg)]
                                       (str "<think>\n"
                                            think
                                            "\n<think>\n"
                                            (:content msg))
                                       (:content msg)))
                       msg->extracted)
        timeout-chan (async/timeout timeout-ms)
        [value port] (async/alts!! [response-chan timeout-chan])]
    (if (= port timeout-chan)
      ;; 如果超时，关闭通道并返回错误
      (do
        (async/close! response-chan)
        {:success false :error "Request timeout"})
      ;; 否则确保返回格式一致的结果
      (if (:success value) 
        (when-let [msg (-> value :response/data  :choices first :message)]
          ;; 安全地执行回调，确保回调中的异常不会影响最终结果
          (when callback
            (try (callback msg)
                 (catch Exception e (println "Callback error:" (.getMessage e)))))
          
          ;; 继续处理结果 
          (try (postfix (extractor-fn msg))
               (catch Exception _ nil)))))))

(defn async-request
  "异步发送请求并返回通道，不阻塞当前线程
   
   Parameters:
   - config 配置参数映射，包含 :receiver 和 :extractor 等选项
     - :receiver 指定接收者，默认为 :default
     - :extractor 提取函数，默认为 :content, 可选值为 :data, :handler, :reasoning_content
   - msgs 要发送的消息列表
   
   Returns:
   - 返回一个 core.async 通道，可以从中获取处理后的响应"
  [{:keys [receiver extractor callback postfix]
    :or {receiver :default
         postfix identity
         extractor :content}} msgs]
  {:pre [(or (every? map? msgs)
             (map? msgs))
         (or (keyword? extractor)
             (fn? extractor))]}
  (let [msgs (if (map? msgs) [msgs] msgs)
        receiver (or receiver :default)
        extractor-fn (case extractor
                       :content #(get % :content)
                       :reasoning_content #(get % :reasoning_content)
                       :data msg->extracted
                       :handler msg->extracted
                       extractor)
        response-chan (client/async-post receiver "/chat/completions" {:messages msgs})]
    (go
      (when-let [value (<! response-chan)]
        (when (:success value)
          (when-let [msg (-> value :response/data :choices first :message)]
            (when callback
              (try (callback msg) (catch Exception e (println "Callback error:" (.getMessage e))))) 
            (try (postfix (extractor-fn msg)) (catch Exception _ nil))))))))

(defn collect-results
  "并行阻塞式等待多个异步操作的结果
   
   Parameters:
   - chans 一组由 async-request 返回的 core.async 通道
   
   Returns:
   - 包含所有通道返回结果的 vector，按输入通道的顺序排列
   - 如果任一通道返回 nil，对应位置也为 nil"
  [& chans]
  (let [results-chan (async/map vector chans)]
    (<!! results-chan)))

;; for quickly use
(defn question-for
  "向指定接收者发送单个问题请求
   
   参数:
   - receiver: 接收者的关键字标识符（如 :default, :gpt-4 等）
   - coll-of-msg: 消息集合，每个元素都会被转换为标准消息格式
   
   返回值:
   - 成功时返回 {:success true :data response}
   - 失败时返回 {:success false :error message}
   
   示例:
   (question-for :default 
     [:user \"你好\"]
     [:system \"你是助手\"])"
  [{:keys [receiver] :or {receiver :default} :as config} & coll-of-msg]
  {:pre [(keyword? receiver)]}
  (try
    (let [msgs (if (s/valid? ::spec/message config)
                 (coll->messages (into [config] coll-of-msg))
                 (coll->messages coll-of-msg))]
      (if (not-empty msgs)
        (if-let [resp (sync-request {:receiver receiver} msgs)]
          (str (:think resp) "\n\n" (:content resp))
          "⚠️ 请求未返回有效响应")
        "⚠️ 没有提供有效消息"))
    (catch Exception e
      (str "⚠️ 错误: " (.getMessage e)))))

(defn extract-question
  "从消息集合中提取用户问题"
  [msgs]
  (->> msgs
       (filter #(= "user" (:role %)))
       (map :content)
       (str/join "\n")))

(defn format-result [{:keys [status error data question timestamp]}]
  (str "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n"
       "❓ 问题：" question "\n"
       "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n"
       "💡 回答："
       (case status
         :complete (-> data :content str)
         :timeout (str "⏱️ 请求超时\n"
                       "   建议：请检查网络连接或增加超时时间")
         :error (str "❌ " error "\n"
                     "   建议：请检查输入格式是否正确")
         :pending "⌛ 等待处理中")
       (when timestamp
         (str "\n🕒 响应时间："
              (.format (java.time.format.DateTimeFormatter/ofPattern
                        "yyyy-MM-dd HH:mm:ss")
                       timestamp)))))

(defn questions-for
  "并行处理多组消息请求，返回包含问题和答案的格式化字符串
   
   参数:
   - receiver: 接收者的关键字标识符
   - config: (可选) 配置映射 {:timeout-ms 超时时间(默认30000)}
   - questions: 多组消息集合，每组都会被转换为标准消息格式
   
   返回值:
   - 返回聚合后的结果，包含所有问答对的格式化字符串"
  [receiver & args]
  (let [[config & questions] (if (map? (first args))
                               args
                               (cons {} args))
        timeout-ms (get config :timeout-ms 30000)
        chans (mapv (fn [q]
                      (let [msgs (coll->messages q)]
                        (async/go
                          (try
                            (let [question (extract-question msgs)
                                  start-time (LocalDateTime/now)
                                  result (sync-request
                                          {:receiver receiver
                                           :timeout-ms timeout-ms}
                                          msgs)]
                              (println "===" result)
                              (if (get result :success)
                                (format-result
                                 {:status :complete
                                  :data result
                                  :question question
                                  :timestamp start-time})
                                (format-result
                                 {:status :timeout
                                  :error (:error result)
                                  :question question
                                  :timestamp start-time})))
                            (catch Exception e
                              (format-result
                               {:status :error
                                :error (.getMessage e)
                                :question (try (extract-question msgs)
                                               (catch Exception _ "无法解析问题"))
                                :timestamp (LocalDateTime/now)}))))))
                    questions)
        results-chan (async/map vector chans)]
    (str/join "\n\n" (<!! results-chan))))
