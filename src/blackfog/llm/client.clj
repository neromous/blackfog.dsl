(ns blackfog.llm.client
  (:require [clj-http.client :as http]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [cheshire.core :as json]
            [clojure.core.async :refer [chan put! go <! >! close!]]
            [clojure.string :as str]
            [blackfog.utils.logger :as log])
  (:import (java.io InputStream)
           (java.nio.charset StandardCharsets)))

;; ================= 配置 =================
(defn load-services-config
  "从 services.edn 加载服务配置"
  []
  (try
    (-> (io/resource "config/services.edn")
        slurp
        edn/read-string)
    (catch Exception e
      (throw (ex-info "Failed to load services config"
                      {:type :config-error
                       :cause (.getMessage e)}
                      e)))))

(def config {:services (load-services-config)})

;; ================= 请求 =================
(defn- get-service-config
  "获取服务配置，如果配置不存在则抛出异常"
  [service-key]
  (if-let [config (get-in config [:services service-key])]
    config
    (throw (ex-info (format "Service '%s' not configured" service-key)
                    {:type :config-error
                     :service service-key}))))

(defn create-request
  "创建 HTTP 请求配置
   
   示例：
   (create-request :openai \"/chat/completions\" {:messages [{:role \"user\" :content \"Hello\"}]})"
  [service-key endpoint body]
  (let [{:keys [api/url api/sk] :as config} (get-service-config service-key)
        ;; 使用 cond-> 按条件添加参数
        request-body
        (-> body
            (cond->
             (:api/model config) (assoc :model (:api/model config))
             (:api/max-retries config) (assoc :max_retries (:api/max-retries config))
             (:api/retry-delay config) (assoc :retry_delay (:api/retry-delay config))
             (:model/temperature config) (assoc :temperature (:model/temperature config))
             (:model/top_p config) (assoc :top_p (:model/top_p config))
             (:model/repetition_penalty config) (assoc :repetition_penalty (:model/repetition_penalty config))
             (:model/presence_penalty config) (assoc :presence_penalty (:model/presence_penalty config))
             (:model/frequency_penalty config) (assoc :frequency_penalty (:model/frequency_penalty config))))]
    (when-not sk
      (throw (ex-info (format "API token not found for service '%s'" service-key)
                      {:type :config-error
                       :service service-key})))
    {:url (str url endpoint)
     :method :post
     :headers {"Authorization" (str "Bearer " sk)
               "Content-Type" "application/json"}
     :body (json/generate-string request-body)
     :as :json
     :socket-timeout 30000
     :connection-timeout 30000}))

(defn- ensure-directory
  "确保目录存在，如不存在则创建"
  [path]
  (let [dir (java.io.File. path)]
    (when-not (.exists dir)
      (.mkdirs dir))
    dir))
(defn save-interaction!
  "保存请求和响应数据到本地文件系统"
  [service-key endpoint request response]
  (try
    (let [now (java.time.Instant/now)
          formatter (-> (java.time.format.DateTimeFormatter/ofPattern "yyyy/MM/dd")
                        (.withZone (java.time.ZoneId/systemDefault)))
          time-formatter (-> (java.time.format.DateTimeFormatter/ofPattern "HH-mm-ss-SSS")
                             (.withZone (java.time.ZoneId/systemDefault)))
          date-path (.format formatter now)
          timestamp (.format time-formatter now)
          base-dir "resources/datasets/history"
          service-dir (str base-dir "/" (name service-key))
          date-dir (str service-dir "/" date-path)
          file-name (str timestamp "-" (java.util.UUID/randomUUID) ".json")
          full-path (str date-dir "/" file-name)
          interaction {:timestamp (.toString now)
                       :service (name service-key)
                       :endpoint endpoint
                       :request (dissoc request :api_key :Authorization)
                       :response response}]

      (ensure-directory date-dir)
      (spit full-path (json/generate-string interaction {:pretty true}))
      (log/debug "Saved interaction to" full-path))
    (catch Exception e
      (log/error "Failed to save interaction:" (ex-message e)))))

(defn async-post
  "执行异步 HTTP POST 请求"
  [service-key endpoint body]
  (let [response-chan (chan)]
    (go
      (try
        (let [request-config (create-request service-key endpoint body)
              response (http/request request-config)
              response-data (:body response)]
          ;; 保存请求和响应
          (save-interaction!
           service-key endpoint (assoc request-config :body body) response-data)
          (put! response-chan {:success true :response/data response-data}))
        (catch Exception e
          (put! response-chan {:success false :error (.getMessage e)}))))
    response-chan))

(defn- create-stream-request
  "创建流式 HTTP 请求配置"
  [service-key endpoint body]
  (try
    (let [base-request (create-request service-key endpoint body)
          ;; 在已生成的请求体基础上添加流式参数
          stream-body (-> (json/parse-string (:body base-request) true)
                          (assoc :stream true)
                          json/generate-string)]
      (merge base-request
             {:as :stream
              :async? true
              :body stream-body}))
    (catch Exception e
      (log/error "Failed to create stream request:" (ex-message e))
      (throw (ex-info "Stream request creation failed"
                      {:cause (ex-message e)}
                      e)))))

(defn- parse-sse-line
  "解析 SSE 行数据，返回解析后的数据或 nil"
  [line]
  (when (and line (str/starts-with? line "data: "))
    (let [data-str (subs line 6)]
      (when-not (= data-str "[DONE]")
        (try
          (json/parse-string data-str true)
          (catch Exception e
            (log/warn "Failed to parse SSE data:" (ex-message e))
            nil))))))

(defn- process-stream
  "处理输入流并将数据发送到通道
   
   返回值格式：
   {:success true/false
    :data parsed-data    ; 当 success 为 true 时
    :error error-message ; 当 success 为 false 时
    :done true/false}    ; 标识流是否结束"
  [^InputStream input-stream out-chan]
  (go
    (try
      (with-open [rdr (-> input-stream
                          (java.io.InputStreamReader. StandardCharsets/UTF_8)
                          java.io.BufferedReader.)]
        (loop []
          (when-let [line (.readLine rdr)]
            (cond
              (= line "data: [DONE]")
              (>! out-chan {:success true :done true})

              (str/starts-with? line "data: ")
              (if-let [parsed (parse-sse-line line)]
                (>! out-chan {:success true :data parsed})
                (>! out-chan {:success false
                              :error "Failed to parse SSE data"})))
            (recur))))
      (catch Exception e
        (>! out-chan {:success false
                      :error (format "Stream processing error: %s" (.getMessage e))}))
      (finally
        (close! out-chan)))))

(defn async-stream-post
  "执行异步流式 HTTP POST 请求
   service-key - 服务标识符
   endpoint - API端点
   body - 请求体"
  [service-key endpoint body]
  (let [out-chan (chan)
        request (-> (create-stream-request service-key endpoint body)
                    (assoc :timeout 300000)  ; 30 秒超时
                    (assoc :idle-timeout 50000))  ; 5 秒空闲超时
        collected-response (atom [])]
    (try
      (http/request
       request
       (fn [response]
         (let [response-chan (chan)]
           ;; 使用中间通道收集响应数据
           (go
             (loop []
               (when-let [chunk (<! response-chan)]
                 (>! out-chan chunk)
                 (when (:success chunk)
                   (when-let [data (:data chunk)]
                     (swap! collected-response conj data)))
                 (when (:done chunk)
                   ;; 流结束时保存完整的交互记录
                   (save-interaction! service-key endpoint request @collected-response))
                 (recur))))
           (process-stream (:body response) response-chan)))
       (fn [exception]
         (put! out-chan {:success false
                         :error (.getMessage exception)})
         (close! out-chan)))
      (catch Exception e
        (put! out-chan {:success false
                        :error (.getMessage e)})
        (close! out-chan)))
    out-chan))

;; 辅助函数：将流式响应转换为完整响应
(defn stream->complete-response
  "将流式响应转换为完整的响应
   返回一个 channel，其中包含组装好的完整响应
   
   示例：
   (go
     (let [response (<! (stream->complete-response
                         (async-stream-post \"/chat/completions\" params)))]
       (if (:success response)
         (println (:content response))
         (println \"Error:\" (:error response)))))"
  [stream-chan]
  (let [result-chan (chan)]
    (go
      (try
        (loop [accumulated-text ""]
          (if-let [response (<! stream-chan)]
            (if (:success response)
              (let [content (-> response :data :choices first :delta :content)]
                (if content
                  (recur (str accumulated-text content))
                  (recur accumulated-text)))
              (>! result-chan response))
            (>! result-chan {:success true :content accumulated-text})))
        (catch Exception e
          (>! result-chan {:success false :error (.getMessage e)}))
        (finally
          (close! result-chan))))
    result-chan))
