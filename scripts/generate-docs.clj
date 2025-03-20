(require '[clojure.java.shell :refer [sh]]
         '[clojure.java.io :as io]
         '[clojure.string :as str])

(def version (or (System/getenv "VERSION") "latest"))
(def docs-dir "target/docs")
(def api-docs-dir (str docs-dir "/api"))

;; 创建文档目录
(defn create-dirs []
  (println "创建文档目录...")
  (-> (io/file docs-dir) .mkdirs)
  (-> (io/file api-docs-dir) .mkdirs))

;; 使用 codox 生成 API 文档
(defn generate-api-docs []
  (println "生成 API 文档...")
  (sh "clojure" "-Sdeps" 
      (str "{:deps {codox/codox {:mvn/version \"0.10.8\"}}}")
      "-m" "codox.main"
      "--output-path" api-docs-dir
      "--name" "BlackFog"
      "--version" version
      "--description" "A flexible, composable DSL framework for knowledge processing and LLM integration"
      "--source-uri" "https://github.com/yourusername/blackfog/blob/{version}/{filepath}#L{line}"
      "--metadata" "{:doc/format :markdown}"
      "--var-meta-whitelist" "[\"^:public\"]"))

;; 生成 HTML 文档索引
(defn generate-index []
  (println "生成文档索引...")
  (spit (str docs-dir "/index.html")
        (str "<!DOCTYPE html>
<html>
<head>
  <meta charset=\"UTF-8\">
  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">
  <title>BlackFog Documentation</title>
  <style>
    body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, Ubuntu, Cantarell, 'Open Sans', 'Helvetica Neue', sans-serif; line-height: 1.6; max-width: 800px; margin: 0 auto; padding: 20px; }
    h1, h2 { border-bottom: 1px solid #eaecef; padding-bottom: 0.3em; }
    a { color: #0366d6; text-decoration: none; }
    a:hover { text-decoration: underline; }
  </style>
</head>
<body>
  <h1>BlackFog Documentation</h1>
  <h2>Version: " version "</h2>
  <ul>
    <li><a href=\"api/index.html\">API Documentation</a></li>
    <li><a href=\"https://github.com/yourusername/blackfog\">GitHub Repository</a></li>
  </ul>
  <h2>Quick Links</h2>
  <ul>
    <li><a href=\"api/blackfog.dsl.core.html\">DSL Core</a></li>
    <li><a href=\"api/blackfog.llm.client.html\">LLM Client</a></li>
  </ul>
</body>
</html>")))

;; 主函数
(defn -main []
  (create-dirs)
  (generate-api-docs)
  (generate-index)
  (println "文档生成完成！可在以下路径查看：" docs-dir))

;; 执行主函数
(-main) 