(ns blackfog.dsl.extractor.common
  (:require [clojure.string :as str]
            [clojure.edn :as edn]
            [blackfog.utils.logger :as log]))

;; 常量定义
(def ^:private patterns
  "正则表达式模式集合
   - :xml          - 通用XML标签匹配
   - :codeblock    - 代码块匹配（包含语言标识）
   - :markdown     - Markdown标题和列表匹配
   - :yaml         - YAML键值对匹配
   - :json         - JSON对象匹配
   - :csv          - CSV行匹配
   - :table        - Markdown表格匹配
   - :file-path    - 文件路径匹配
   - :image-link   - Markdown图片链接匹配
   - :url-link     - Markdown URL链接匹配
   - :bash-command - bash命令匹配
   - :code-citation - 代码引用块匹配"
  {:xml            #"(?s)<(\w+)>((?:(?!</\1>).)*)</\1>"
   :codeblock      #"```(\w+)\n([\s\S]*?)\n```"
   :markdown-h     #"(#{1,6})\s+(.+)$"
   :markdown-list  #"[-*+]\s+(.+)$"
   :yaml           #"(\w+):\s*(.+)$"
   :json           #"\{\s*\"(.+?)\":\s*(.+?)\s*\}"
   :csv            #"([^,\n]+),([^,\n]+)(?:,([^,\n]+))*"
   :table          #"\|(.+?)\|(.+?)\|(?:(.+?)\|)*\n\|[-:]+\|[-:]+\|(?:[-:]+\|)*\n(\|.+\n)+"
   :file-path      #"(?:^|\s)(?:[~\w\-\.\/\\]+\/)*[\w\-\.]+\.[a-zA-Z0-9]{1,4}(?=\s|$)"
   :image-link     #"!\[([^\]]*)\]\(([^)]+)\)"
   :url-link       #"\[([^\]]+)\]\(([^)]+)\)"
   :bash-command   #"(?:^|\n)\s*[$>]\s*([^\n]+)"
   :code-citation  #"```(\d+):(\d+):([^\n]+)\n"})

;; 辅助函数
(defn- trim-and-normalize
  "标准化文本内容，去除多余空格"
  [content]
  (-> content
      str/trim
      (str/replace #"[\r\n]{3,}" "\n\n")))

(defn content->xml
  "Parse XML-like content
   Parameters:
   - content: String  containing XML-like markup
   Returns: Vector of [tag content] pairs"
  [content]
  (let [pattern (:xml patterns)
        matches (re-seq pattern content)]
    (when (empty? matches)
      (throw (ex-info "XML parsing failed"
                      {:type  :parse-error
                       :input content})))
    (mapv (fn [[_ tag content]]
            [tag
             (trim-and-normalize content)])
          matches)))

(defn content->codeblock
  "Parse Codeblock-like content 
   Parameters:
   - content: String may containing codeblock markup
   Returns: Vector of [language content] pairs"
  [content]
  (let [pattern (:codeblock patterns)
        matches (re-seq pattern content)]
    (when (empty? matches)
      (throw (ex-info "Codeblock parsing failed"
                      {:type  :parse-error
                       :input content})))
    (mapv (fn [[_ lang code]]
            [(keyword lang) (str/trim code)])
          matches)))

(defn content->markdown
  "提取Markdown格式内容
   Parameters:
   - content: 包含Markdown格式的字符串
   Returns: 格式化后的内容向量"
  [content]
  (let [headers (re-seq (:markdown-h patterns) content)
        lists   (re-seq (:markdown-list patterns) content)]
    (concat
     (mapv (fn [[_ level text]]
             [(keyword (str "h" (count level)))
              (trim-and-normalize text)])
           headers)
     (mapv (fn [[_ text]]
             [:li (trim-and-normalize text)])
           lists))))

(defn content->yaml
  "提取YAML格式内容
   Parameters:
   - content: 包含YAML格式的字符串
   Returns: 键值对向量"
  [content]
  (let [matches (re-seq (:yaml patterns) content)]
    (mapv (fn [[_ key value]]
            [(keyword key) (trim-and-normalize value)])
          matches)))

(defn content->json
  "提取JSON格式内容
   Parameters:
   - content: 包含JSON格式的字符串
   Returns: 解析后的JSON对象"
  [content]
  (try
    (cheshire.core/parse-string content true)
    (catch Exception e
      (try
        ;; 如果整体解析失败，尝试使用正则表达式提取键值对
        (let [matches (re-seq (:json patterns) content)]
          (into {}
                (map (fn [[_ key value]]
                       [(keyword key)
                        (try
                          (cheshire.core/parse-string value true)
                          (catch Exception _
                            value))])
                     matches)))
        (catch Exception e
          (log/warn "JSON parsing failed:" (.getMessage e))
          nil)))))

(defn content->edn
  "提取JSON格式内容
   Parameters:
   - content: 包含JSON格式的字符串
   Returns: 解析后的数据结构"
  [content]
  (try
    (let [result (edn/read-string content)]
      (if (or (vector? result) (map? result))
        result))
    (catch Exception e
      (log/warn "EDN parsing failed:" (.getMessage e))
      nil)))

(defn content->table
  "提取Markdown表格内容
   Parameters:
   - content: 包含表格的字符串
   Returns: 表格数据结构"
  [content]
  (when-let [table-match (re-find (:table patterns) content)]
    (let [lines (str/split-lines table-match)
          header-line (first lines)
          headers (mapv str/trim (str/split (subs header-line 1 (dec (count header-line))) #"\|"))
          data-lines (drop 2 lines)]
      {:headers headers
       :rows (mapv (fn [line]
                     (mapv str/trim
                           (str/split (subs line 1 (dec (count line))) #"\|")))
                   data-lines)})))

;; 新增提取器函数
(defn content->file-paths
  "提取文本中的文件路径
   Returns: 文件路径列表"
  [content]
  (let [matches (re-seq (:file-path patterns) content)]
    (mapv (fn [[path]] [:file-path path]) matches)))

(defn content->image-links
  "提取Markdown格式的图片链接
   Returns: [alt-text url] 对的向量"
  [content]
  (let [matches (re-seq (:image-link patterns) content)]
    (mapv (fn [[_ alt url]]
            [:image {:alt alt :url url}])
          matches)))

(defn content->urls
  "提取Markdown格式的URL链接
   Returns: [text url] 对的向量"
  [content]
  (let [matches (re-seq (:url-link patterns) content)]
    (mapv (fn [[_ text url]]
            [:url {:text text :url url}])
          matches)))

(defn content->bash-commands
  "提取bash命令
   Returns: 命令列表"
  [content]
  (let [matches (re-seq (:bash-command patterns) content)]
    (mapv (fn [[_ cmd]]
            [:bash-command (str/trim cmd)])
          matches)))

(defn content->code-citations
  "提取代码引用块
   Returns: [{:start line1 :end line2 :file path}]"
  [content]
  (let [matches (re-seq (:code-citation patterns) content)]
    (mapv (fn [[_ start end file]]
            [:code-citation {:start (Integer/parseInt start)
                             :end (Integer/parseInt end)
                             :file file}])
          matches)))



