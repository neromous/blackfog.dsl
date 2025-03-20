;;  handlers的格式为
;; [[:file/list-dir "playground"]
;;  [:file/read-text "playground/test.txt"]]
;; handler 的格式为 [tag handler-body]

(ns blackfog.dsl.handler.core
  "主要针对提取器中xml标签解析出的数据进行处理"
  (:require [blackfog.dsl.core :refer [render is-form?]]))

(defmulti dispatch (fn [tag _] tag))

(defmethod dispatch :default
  [_ handler] handler)

(defmethod dispatch :action 
  [_ handler]
  (if (is-form? handler) (render handler) nil))

(defn apply-handler
  "应用单个handler
   Parameters:
   - handler: [tag content] 格式的handler
   Returns: 处理结果"
  [[tag handler-body]]
  (try
    (str "\n>>>执行`[" tag " " handler-body "]`的结果为:\n"
     (dispatch tag handler-body))
    (catch Exception e
      (println "Handler failed:" tag (.getMessage e))
      nil)))

(defn apply-handlers
  "应用多个handlers
   Parameters:
   - handlers: [[tag content]...] 格式的handlers序列
   - allowed-tags: #{tag1 tag2 ...} 允许执行的tag集合，如果为nil，则不进行过滤
   Returns: 处理结果的vector,过滤掉nil结果"
  [handlers & {:keys [allowed-tags] :or {allowed-tags #{:action}}}]
  (let [filtered-handlers (filter (comp allowed-tags first) handlers)]
    (->> filtered-handlers
         (map apply-handler)
         (remove nil?)
         (vec))))
