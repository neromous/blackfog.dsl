(ns blackfog.utils.data-formatter.xml
  (:require [clojure.string :as str]
            [clojure.edn :as edn]
            [clojure.data.xml :as xml]
            [clojure.zip :as zip]
            [clojure.data.zip.xml :as zip-xml]
            [blackfog.utils.logger :as log])
  (:import [java.io PushbackReader StringReader File]))

(defn xml-string?
  "判断字符串是否为有效的XML格式"
  [s]
  (and (string? s)
       (str/starts-with? (str/trim s) "<")
       (str/ends-with? (str/trim s) ">")))

(defn file-exists?
  "检查文件是否存在"
  [path]
  (when (string? path)
    (let [file (File. ^String path)]
      (.exists file))))

(defn parse-xml-string
  "解析XML字符串为XML数据结构"
  [xml-str]
  (try
    (xml/parse (StringReader. xml-str))
    (catch Exception e
      (log/error "解析XML字符串失败:" (.getMessage e))
      nil)))

(defn parse-xml-file
  "从文件中读取并解析XML"
  [file-path]
  (try
    (xml/parse (File. ^String file-path))
    (catch Exception e
      (log/error "解析XML文件失败:" (.getMessage e) "文件路径:" file-path)
      nil)))

(defn xml->zipper
  "将XML转换为可导航的zipper结构"
  [xml]
  (zip/xml-zip xml))

(defn xml->map
  "将XML转换为Clojure map结构"
  [xml]
  (letfn [(process-contents [contents]
            (let [texts (filter string? contents)
                  nodes (filter (complement string?) contents)
                  text (when (seq texts) (str/join "" texts))
                  children (map node->map nodes)]
              (cond-> {}
                (seq text) (assoc :text text)
                (seq children) (assoc :children children))))
          (node->map [node]
            (if (string? node)
              node
              (let [{:keys [tag attrs content]} node]
                (cond-> {:tag tag}
                  (seq attrs) (assoc :attrs attrs)
                  (seq content) (merge (process-contents content))))))]
    (node->map xml)))

(defn parse-xml
  "智能解析XML，自动判断输入是文件路径还是XML字符串
   返回解析后的XML结构"
  [input]
  (cond
    (xml-string? input) (parse-xml-string input)
    (file-exists? input) (parse-xml-file input)
    :else (do
            (log/warn "无法识别的XML输入格式")
            nil)))

(defn parse-xml->zipper
  "智能解析XML并返回zipper结构，方便使用clojure.data.zip.xml进行查询"
  [input]
  (when-let [xml (parse-xml input)]
    (xml->zipper xml)))

(defn parse-xml->map
  "智能解析XML并转换为Clojure map结构"
  [input]
  (when-let [xml (parse-xml input)]
    (xml->map xml)))

