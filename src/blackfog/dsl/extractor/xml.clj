(ns blackfog.dsl.extractor.xml
  "针对已经提取的xml数据进行进一步解析，这是业务化解析，所以根据标签来切换
   返回内容为[[tag content]...] 的序列，每个元素为[tag content]，tag为标签，content为内容"
  (:require [clojure.data.xml :as xml]
            [clojure.string :as str]
            [blackfog.dsl.core :refer [is-form?]]
            [blackfog.dsl.extractor.common :refer [content->edn]]))

(defn content->handlers
  "action标签的内容是action的配置，可能是一个action，也可能是一组action,统一返回handlers, 失误了侧"
  [tag content]
  (let [result (content->edn content)]
    (cond
      (is-form? result) [[tag result]]
      (every? is-form? result) (into [] (for [r result] [tag r]))
      :else nil)))

;; 定义多种方法
(defmulti xml-extractor (fn [tag _] tag))

(defmethod xml-extractor :default  
  [_ content]
  [[:raw content]])

(defmethod xml-extractor :action  
  [_ content]
  (try (content->handlers :action content)
    (catch Exception _ nil)))

(defmethod xml-extractor :iphone  [tag content]
  (try (content->handlers :action content)
       (catch Exception _ nil)))

(defmethod xml-extractor :repl  [tag content]
  (try [[:repl (read-string content)]]
       (catch Exception _ nil)))

(defmethod xml-extractor :bash  [_ content]
  (try 
    (let [res (->> (str/split-lines content)
                   (map (fn [line]
                          (cond-> line
                            ;; 去掉行首的$>
                            (re-find #"^\s*[$>]\s+" line) 
                            (str/replace #"^\s*[$>]\s+" ""))))
                   (str/join "\n"))]
      [[:bash res]])
    (catch Exception _ nil)))

