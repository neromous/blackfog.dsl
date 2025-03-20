(ns blackfog.dsl.func.http
  (:require [clj-http.client :as http]
            [clojure.data.json :as json]
            [clojure.string :as str])
  (:import [org.jsoup Jsoup]))

(defn- url-encode
  "对URL中的中文和特殊字符进行编码"
  [url]
  (if (nil? url)
    nil
    (java.net.URLEncoder/encode (str url) "UTF-8")))

(defn- safe-url
  "确保URL中的中文和特殊字符被正确编码"
  [url]
  (if (nil? url)
    nil
    (let [parts (str/split url #"/")
          encoded-parts (map (fn [part]
                               (if (and (not (str/blank? part))
                                        (not (str/starts-with? part "http"))
                                        (not (str/starts-with? part "https")))
                                 (url-encode part)
                                 part))
                             parts)]
      (str/join "/" encoded-parts))))

;; ================ 辅助函数 ================

(defn- format-http-response
  "将HTTP响应格式化为易读的文本"
  [response]
  (let [status (:status response)
        headers (:headers response)
        body (:body response)
        content-type (get headers "content-type" "")]
    (str "🌐 HTTP 响应:\n\n"
         "📊 状态码: " status "\n"
         "⏱️ 响应时间: " (get response :request-time "未知") "ms\n\n"
         "📄 响应内容:\n"
         (cond
           (str/includes? content-type "application/json")
           (try
             (let [json-data (json/read-str body :key-fn keyword)]
               (str "\n```json\n" (json/write-str json-data {:indent true}) "\n```"))
             (catch Exception _
               (str "\n" (if (> (count body) 500)
                           (str (subs body 0 500) "...(内容已截断)")
                           body))))

           (str/includes? content-type "text/html")
           (let [doc (Jsoup/parse body)
                 plain-text (.text doc)]
             (str "\n(HTML内容，长度: " (count body) " 字符)"
                  "\n" (if (> (count plain-text) 5000)
                         (str (subs plain-text 0 5000) "...(内容已截断)")
                         plain-text)))

           :else
           (str "\n" (if (> (count body) 500)
                       (str (subs body 0 500) "...(内容已截断)")
                       body))))))

(defn- format-search-results
  "将搜索结果格式化为易读的文本"
  [results]
  (str "🔍 搜索结果:\n\n"
       (str/join "\n\n"
                 (map-indexed
                  (fn [idx result]
                    (str "📌 结果 #" (inc idx) "\n"
                         "📝 标题: " (or (:title result) "[无标题]") "\n"
                         "🔗 链接: " (or (:url result) "[无链接]") "\n"
                         "📄 摘要: " (or (:snippet result) "[无摘要]")))
                  results))))

;; ================ 网络系统 ================

(defn http-get [url & [options]]
  (try
    (let [default-options {:socket-timeout 10000
                           :conn-timeout 10000
                           :accept :json
                           :as :text}
          merged-options (merge default-options (or options {}))
          encoded-url (safe-url url)
          response (http/get encoded-url merged-options)]
      (format-http-response response))
    (catch Exception e
      (str "❌ HTTP GET 请求失败: " (.getMessage e)))))


(defn http-post [url data & [options]]
  (try
    (let [default-options {:socket-timeout 10000
                           :conn-timeout 10000
                           :content-type :json
                           :accept :json
                           :as :text
                           :body (if (string? data)
                                   data
                                   (json/write-str data))}
          merged-options (merge default-options (or options {}))
          encoded-url (safe-url url)
          response (http/post encoded-url merged-options)]
      (format-http-response response))
    (catch Exception e
      (str "❌ HTTP POST 请求失败: " (.getMessage e)))))

;; ================ 搜索系统 ================

(defn http-api [service endpoint params]
  (try
    (let [service-config (case service
                           :weather {:base-url "https://api.openweathermap.org/data/2.5"
                                     :api-key "YOUR_API_KEY"
                                     :method :get}
                           :news {:base-url "https://newsapi.org/v2"
                                  :api-key "YOUR_API_KEY"
                                  :method :get}
                           :translation {:base-url "https://translation.googleapis.com/language/translate/v2"
                                         :api-key "YOUR_API_KEY"
                                         :method :post}
                           (throw (Exception. (str "未知服务: " service))))

          url (str (:base-url service-config) "/" endpoint)
          encoded-url (safe-url url)
          method (:method service-config)
          api-key (:api-key service-config)

          options (if (= method :get)
                    {:query-params (assoc params :apiKey api-key)}
                    {:form-params (assoc params :key api-key)})

          response (if (= method :get)
                     (http/get encoded-url options)
                     (http/post encoded-url options))]

      (format-http-response response))
    (catch Exception e
      (str "❌ API 调用失败: " (.getMessage e)))))

(defn web-search [query & [limit]]
  (try
    (let [search-url "https://api.bing.microsoft.com/v7.0/search"
          api-key "YOUR_BING_API_KEY"
          max-results (or limit 5)

          response (http/get search-url
                             {:headers {"Ocp-Apim-Subscription-Key" api-key}
                              :query-params {:q query :count max-results}
                              :as :json})

          ;; 尝试从响应中获取结果，处理不同格式的响应
          results (cond
                    ;; 1. 响应体已经是map (clj-http处理了:as :json)
                    (map? (:body response))
                    (get-in response [:body :webPages :value])
                    
                    ;; 2. 响应体是string，需要解析JSON
                    (string? (:body response))
                    (try
                      (let [parsed-body (json/read-str (:body response) :key-fn keyword)]
                        (get-in parsed-body [:webPages :value]))
                      (catch Exception _ nil))
                    
                    ;; 3. 其他情况
                    :else nil)
          
          ;; 处理结果，提取所需字段，注意字段名称的映射
          formatted-results (map (fn [result]
                                   {:title (or (:title result) (:name result))
                                    :url (:url result)
                                    :snippet (or (:snippet result) (:description result))})
                                 (or results []))]

      (if (seq formatted-results)
        (format-search-results formatted-results)
        "❌ 无搜索结果或结果格式异常"))
    (catch Exception e
      (str "❌ 网络搜索失败: " (.getMessage e)))))
