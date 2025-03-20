(ns blackfog.dsl.http-test
  (:require [clojure.test :refer [deftest testing is are]]
            [clojure.string :as str]
            [blackfog.dsl.core :refer [reg-element render]]
            [blackfog.dsl.register]
            [clj-http.client :as http]
            [clojure.data.json :as json])
  (:import [java.io ByteArrayInputStream]))

;; 模拟HTTP响应
(defn- mock-response [status body & [opts]]
  (let [defaults {:status status
                  :headers {"content-type" "application/json"}
                  :body body
                  :request-time 123}]
    (merge defaults (or opts {}))))

;; 基本HTTP DSL功能测试
(deftest http-dsl-basic-test
  (testing "HTTP DSL函数注册检查"
    (is (fn? (get @blackfog.dsl.core/element-registry! :http/get)) "http/get应已注册")
    (is (fn? (get @blackfog.dsl.core/element-registry! :http/post)) "http/post应已注册")
    (is (fn? (get @blackfog.dsl.core/element-registry! :http/api)) "http/api应已注册")
    (is (fn? (get @blackfog.dsl.core/element-registry! :http/web)) "http/web应已注册")))

;; HTTP GET请求测试
(deftest http-get-test
  (testing "HTTP GET请求 - 成功情况"
    (with-redefs [http/get (fn [url opts]
                             (is (= "https://api.example.com/data" url) "URL应正确传递")
                             (mock-response 200 "{\"success\":true,\"data\":\"测试数据\"}"))]
      (let [result (render [:http/get "https://api.example.com/data"])]
        (is (string? result) "HTTP GET应返回字符串")
        (is (.contains result "🌐 HTTP 响应") "响应应包含HTTP响应标识")
        (is (.contains result "📊 状态码: 200") "响应应包含状态码")
        (is (.contains result "\"success\"") "响应应包含success字段")
        (is (.contains result "\"data\"") "响应应包含data字段"))))
  
  (testing "HTTP GET请求 - 错误情况"
    (with-redefs [http/get (fn [url opts]
                             (throw (Exception. "连接超时")))]
      (let [result (render [:http/get "https://api.example.com/error"])]
        (is (string? result) "错误情况下应返回字符串")
        (is (.contains result "❌ HTTP GET 请求失败") "应包含错误标识")
        (is (.contains result "连接超时") "应包含错误信息")))))

;; HTTP POST请求测试
(deftest http-post-test
  (testing "HTTP POST请求 - 成功情况"
    (with-redefs [http/post (fn [url opts]
                              (is (= "https://api.example.com/submit" url) "URL应正确传递")
                              ;; 更灵活地检查请求体
                              (is (string? (:body opts)) "请求体应为字符串")
                              (is (or (.contains (str (:body opts)) "测试数据")
                                      (.contains (str (:body opts)) "\"name\""))
                                  "请求体应包含相关数据")
                              (mock-response 201 "{\"success\":true,\"message\":\"创建成功\"}"))]
      (let [result (render [:http/post "https://api.example.com/submit" {:name "测试数据"}])]
        (is (string? result) "HTTP POST应返回字符串")
        (is (.contains result "🌐 HTTP 响应") "响应应包含HTTP响应标识")
        (is (.contains result "📊 状态码: 201") "响应应包含状态码")
        (is (.contains result "\"success\"") "响应应包含success字段")
        (is (.contains result "\"message\"") "响应应包含message字段"))))
  
  (testing "HTTP POST请求 - 错误情况"
    (with-redefs [http/post (fn [url opts]
                              (throw (Exception. "服务器错误")))]
      (let [result (render [:http/post "https://api.example.com/error" {:test "data"}])]
        (is (string? result) "错误情况下应返回字符串")
        (is (.contains result "❌ HTTP POST 请求失败") "应包含错误标识")
        (is (.contains result "服务器错误") "应包含错误信息")))))

;; API调用测试
(deftest http-api-test
  (testing "API调用 - 天气服务"
    (with-redefs [http/get (fn [url opts]
                             (is (.contains url "api.openweathermap.org") "应调用天气API")
                             (mock-response 200 "{\"weather\":[{\"main\":\"Clear\"}],\"main\":{\"temp\":25}}"))]
      (let [result (render [:http/api :weather "weather" {:q "Beijing"}])]
        (is (string? result) "API调用应返回字符串")
        (is (.contains result "Clear") "响应应包含天气信息")
        (is (.contains result "temp") "响应应包含温度信息"))))
  
  (testing "API调用 - 错误情况"
    (with-redefs [http/get (fn [url opts]
                             (throw (Exception. "API密钥无效")))]
      (let [result (render [:http/api :weather "forecast" {:q "Invalid"}])]
        (is (string? result) "错误情况下应返回字符串")
        (is (.contains result "❌ API 调用失败") "应包含错误标识")
        (is (.contains result "API密钥无效") "应包含错误信息")))))

;; Web搜索测试
(deftest web-search-test
  (testing "Web搜索 - 成功情况"
    (with-redefs [http/get (fn [url opts]
                             (is (.contains url "bing.microsoft.com") "应调用Bing搜索API")
                             (is (= "Clojure编程" (get-in opts [:query-params :q])) "搜索词应正确传递")
                             (mock-response 200 (json/write-str
                                                 {:webPages
                                                  {:value
                                                   [{:name "Clojure官网"
                                                     :url "https://clojure.org"
                                                     :snippet "Clojure是一种函数式编程语言"}
                                                    {:name "Clojure教程"
                                                     :url "https://example.com/clojure"
                                                     :snippet "学习Clojure的最佳资源"}]}})))]
      (let [result (render [:http/web "Clojure编程"])]
        (is (string? result) "Web搜索应返回字符串")
        (is (.contains result "搜索结果") "响应应包含搜索结果标识")
        (is (.contains result "Clojure官网") "响应应包含搜索结果标题")
        (is (.contains result "https://clojure.org") "响应应包含搜索结果URL")
        (is (.contains result "函数式编程语言") "响应应包含搜索结果摘要"))))
  
  (testing "Web搜索 - 错误情况"
    (with-redefs [http/get (fn [url opts]
                             (throw (Exception. "搜索服务不可用")))]
      (let [result (render [:http/web "错误搜索"])]
        (is (string? result) "错误情况下应返回字符串")
        (is (.contains result "❌ 网络搜索失败") "应包含错误标识")
        (is (.contains result "搜索服务不可用") "应包含错误信息")))))

;; HTTP与其他DSL组合测试
(deftest http-integration-test
  (testing "HTTP结果与样式组合"
    (with-redefs [http/get (fn [url opts]
                             (mock-response 200 "{\"weather\":\"晴天\",\"temperature\":25}"))]
      (let [result (render [:card
                            [:h3 "天气信息"]
                            [:p [:bold "查询结果:"]]
                            [:p (render [:http/get "https://api.example.com/weather"])]])]
        (is (string? result) "组合渲染应返回字符串")
        (is (.contains result "天气信息") "应包含卡片标题")
        (is (.contains result "🌐 HTTP 响应") "应包含HTTP响应标识")
        (is (.contains result "\"weather\"") "应包含weather字段")
        (is (.contains result "\"temperature\"") "应包含temperature字段"))))
  
  (testing "HTTP结果与变量绑定"
    (with-redefs [http/get (fn [url opts]
                             (mock-response 200 "{\"location\":\"北京\",\"weather\":\"多云\"}"))]
      (let [weather-data (render [:http/get "https://api.example.com/weather"])
            result (render {'?weather-data weather-data}
                           [:rows
                            [:h2 "今日天气预报"]
                            [:p "数据来源: 天气API"]
                            [:p [:bold "详细信息:"]]
                            [:p '?weather-data]])]
        (is (string? result) "变量绑定渲染应返回字符串")
        (is (.contains result "今日天气预报") "应包含标题")
        (is (.contains result "🌐 HTTP 响应") "应包含HTTP响应标识")
        (is (.contains result "\"location\"") "应包含location字段")
        (is (.contains result "\"weather\"") "应包含weather字段")))))

;; 综合HTTP功能测试
(deftest comprehensive-http-test
  (testing "多个HTTP请求组合"
    (with-redefs [http/get (fn [url opts]
                             (cond
                               (.contains url "weather") 
                               (mock-response 200 "{\"weather\":\"晴天\"}")
                               
                               (.contains url "news") 
                               (mock-response 200 "{\"headlines\":[\"今日新闻\"]}")
                               
                               :else 
                               (mock-response 404 "Not found")))]
      (let [weather (render [:http/get "https://api.example.com/weather"])
            news (render [:http/get "https://api.example.com/news"])
            report (render [:rows
                            [:h1 "每日简报"]
                            [:h2 "天气情况"]
                            [:p weather]
                            [:h2 "新闻头条"]
                            [:p news]])]
        (is (string? report) "综合报告应返回字符串")
        (is (.contains report "每日简报") "应包含报告标题")
        (is (.contains report "天气情况") "应包含天气部分")
        (is (.contains report "\"weather\"") "应包含weather字段")
        (is (.contains report "新闻头条") "应包含新闻部分")
        (is (.contains report "\"headlines\"") "应包含headlines字段")))))
