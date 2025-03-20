(ns blackfog.dsl.extractor.codeblock 
  "针对已经提取的xml数据进行进一步解析，这是业务化解析，所以根据标签来切换
   返回内容为[[tag content]...] 的序列，每个元素为[tag content]，tag为标签，content为内容"
  (:require
   [clojure.edn :as edn]
   [cheshire.core :as json]
   [clojure.walk :as walk]
   [clojure.data.csv :as csv]
   [clojure.data.xml :as xml]
   [clojure.string :as string]))

(defmulti codeblock-extractor (fn [tag content] tag))

(defmethod codeblock-extractor :default [tag content] [tag content])

(defmethod codeblock-extractor :edn
  [_ content]
  (try
    [[:data (edn/read-string content)]]
    (catch Exception _ nil)))

(defmethod codeblock-extractor :json
  [_ content]
  (try
    [[:data (json/parse-string content true)]]
    (catch Exception _ nil)))

(defmethod codeblock-extractor :csv
  [_ content]
  (try
    [[:data (with-open [reader (java.io.StringReader. content)]
             (doall (csv/read-csv reader)))]]
    (catch Exception _ nil)))

(defmethod codeblock-extractor :xml
  [_ content]
  (try
    [[:data (xml/parse-str content)]]
    (catch Exception _
      nil)))

(defmethod codeblock-extractor :clojure [_ content]
  (try
    [[:clojure/code (read-string content)]]
    (catch Exception _
      nil)))

(defmethod codeblock-extractor :markdown [_ content]
  (try
    [[:markdown content]]
    (catch Exception _
      nil)))

(defmethod codeblock-extractor :bash
  [_ content]
  (try
    [[:bash/script
     (->> (string/split-lines content)
          (map (fn [line]
                 (cond-> line
                   (re-find #"^\s*[$>]\s+" line) (string/replace #"^\s*[$>]\s+" ""))))
          (string/join "\n"))]]
    (catch Exception _
      nil)))

