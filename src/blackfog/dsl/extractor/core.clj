(ns blackfog.dsl.extractor.core
  "消息内容提取器核心模块
   提供了一个可扩展的提取器框架，支持多种格式的消息内容提取，每个提取都是通过函数而不是正则表达式，
   原因是函数要比正则表达式更灵活，更易于维护。
   提取来自LLM推理生成的文本数据"
  (:require
   [clojure.string :as str]
   [blackfog.utils.logger :as log]
   [blackfog.dsl.extractor.common :as common-extract]
   [blackfog.dsl.extractor.codeblock :refer [codeblock-extractor]]
   [blackfog.dsl.extractor.xml :refer [xml-extractor]]))

;; ==================== 标准解析器定义 ====================
(def common-extractors
  "因为我们也不知道到底content里面有什么，所以提取全部类型
   默认提取器映射，将提取器类型映射到对应的提取函数
   - :xml - XML格式提取
   - :codeblock - 代码块提取
   - :markdown - Markdown格式提取
   - :yaml - YAML格式提取
   - :json - JSON格式提取
   - :table - 表格格式提取"
  {:xml common-extract/content->xml
   :codeblock common-extract/content->codeblock
   :markdown common-extract/content->markdown
   :yaml common-extract/content->yaml
   :json common-extract/content->json
   :table common-extract/content->table
   :edn common-extract/content->edn
   :file-paths common-extract/content->file-paths
   :image-links common-extract/content->image-links
   :urls common-extract/content->urls
   :bash-commands common-extract/content->bash-commands
   :code-citations common-extract/content->code-citations})

;; ==================== 辅助函数 ====================
(defn- apply-extractor
  "应用单个标准提取器并返回标准化结果"
  [type extractor-fn content]
  (try
    (let [result (extractor-fn content)]
      (if (or (nil? result) (and (coll? result) (empty? result)))
        {:success false :error (str "Empty result from " type) :method type}
        {:success true :data result :method type}))
    (catch Exception e
      {:success false
       :method type
       :error (ex-message e)})))

(defn apply-extractors
  "并行应用所有标准提取器
   使用 pmap 实现并行处理，提高大量内容处理效率"
  [content extractors]
  (let [extractor-pairs (seq extractors)
        results (pmap (fn [[type extractor-fn]]
                        [type (apply-extractor type extractor-fn content)])
                      extractor-pairs)]
    (persistent!
     (reduce
      (fn [acc [type result]]
        (let [{:keys [success data error details]} result]
          (if success
            (assoc! acc type data)
            (do
              (log/debug "Extraction failed for type" type
                         {:error error :details details})
              acc))))
      (transient {})
      results))))

;; 对标准解析器结果应用扩展解析器
(defn apply-extended-extractors
  "对标准解析器的结果应用扩展解析器
   Parameters:
   - extraction-results: 标准解析器的结果
   Returns: 应用扩展解析器后的结果"
  [extraction-results]
  (let [result (transient {})]
    ;; 处理代码块提取结果
    (when-let [codeblocks (get extraction-results :codeblock)]
      (doseq [[tag content] codeblocks]
        (try
          (let [handlers (get extraction-results :handlers [])
                extracted (codeblock-extractor tag content)]
            (assoc! result :handlers (into handlers extracted)))
          (catch Exception e
            (log/warn "Failed to extract codeblock" tag (.getMessage e))))))

    ;; 处理XML提取结果
    (when-let [xml-data (get extraction-results :xml)]
      (doseq [[tag content] xml-data]
        (try
          (let [handlers  (get result :handlers [])
                ;; 解析后的多个
                extracted (xml-extractor tag content)]
            (when extracted
              (assoc! result :handlers (into handlers extracted))))
          (catch Exception e
            (log/warn "Failed to extract XML" tag (.getMessage e))))))

    ;; 合并结果
    (merge extraction-results (persistent! result))))

(defn extract-content
  "提取消息内容中的各种格式数据
   Parameters:
   - content: 要提取的消息内容
   - extractors: 可选，自定义提取器映射
   Returns: 包含所有提取结果的映射"
  ([content]
   (extract-content content common-extractors))
  ([content extractors]
   (let [standard-results (apply-extractors content extractors)
         extended-results (apply-extended-extractors standard-results)]
     extended-results)))



