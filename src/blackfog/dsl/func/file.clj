(ns blackfog.dsl.func.file
  (:require [blackfog.dsl.core :refer [reg-element]]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.data.json :as json]
            [clojure.edn :as edn]
            [clojure.data.csv :as csv])
  (:import [java.io File ByteArrayOutputStream]
           [java.nio.file Files Paths]
           [java.util Base64]
           [java.awt.image BufferedImage]
           [javax.imageio ImageIO]
           [javax.sound.sampled AudioSystem AudioFormat AudioInputStream]
           [org.apache.tika Tika]
           [org.jcodec.api FrameGrab JCodecException]
           [org.jcodec.common.model Picture]
           [org.jcodec.scale AWTUtil]
           [org.jcodec.containers.mp4.demuxer MP4Demuxer]
           [org.jcodec.common.io NIOUtils]
           [org.jcodec.common DemuxerTrack]
           [org.jcodec.common.model Packet]))

;; ================ 辅助函数 ================

(defn *file-exists?
  "检查文件是否存在"
  [path]
  (.exists (io/file path)))

(defn *get-file-info
  "获取文件基本信息"
  [path]
  (let [file (io/file path)]
    (when (.exists file)
      {:name (.getName file)
       :path (.getAbsolutePath file)
       :size (.length file)
       :last-modified (.lastModified file)
       :is-directory (.isDirectory file)
       :is-file (.isFile file)})))

(defn *get-file-type
  "使用 Tika 检测文件类型"
  [path]
  (try
    (let [tika (Tika.)
          file (io/file path)]
      (.detect tika file))
    (catch Exception _
      "unknown/unknown")))

(defn *format-file-info
  "格式化文件信息为易读文本"
  [file-info]
  (when file-info
    (str "📁 文件信息:\n\n"
         "📝 名称: " (:name file-info) "\n"
         "📂 路径: " (:path file-info) "\n"
         "📊 大小: " (:size file-info) " 字节\n"
         "🕒 最后修改: " (java.util.Date. (:last-modified file-info)) "\n"
         "📑 类型: " (if (:is-directory file-info) "目录" "文件"))))

(defn file-info [path]
  (if (*file-exists? path)
    (let [file-info (*get-file-info path)
          file-type (*get-file-type path)]
      (str (*format-file-info file-info)
           "\n📄 MIME类型: " file-type))
    (str "❌ 文件不存在: " path)))

(defn list-dir [path]
  (try
    (let [file (io/file path)]
      (if (and (.exists file) (.isDirectory file))
        (let [files (.listFiles file)
              dirs (filter #(.isDirectory %) files)
              regular-files (filter #(.isFile %) files)]
          (str "📂 目录: " path "\n\n"
               "📁 子目录 (" (count dirs) "):\n"
               (if (seq dirs)
                 (str/join "\n" (map #(str "   📁 " (.getName %)) dirs))
                 "   (无子目录)")
               "\n\n"
               "📄 文件 (" (count regular-files) "):\n"
               (if (seq regular-files)
                 (str/join "\n" (map #(str "   📄 " (.getName %)) regular-files))
                 "   (无文件)")))
        (str "❌ 路径不存在或不是目录: " path)))
    (catch Exception e
      (str "❌ 目录读取失败: " (.getMessage e)))))

;; ================ 文本文件操作 ================

(defn- *read-text-file-full
  "读取整个文本文件"
  [path]
  (try
    (slurp path)
    (catch Exception e
      (str "❌ 文件读取失败: " (.getMessage e)))))

(defn read-text-file-full [path]
  (if (*file-exists? path)
    (let [content (*read-text-file-full path)
          file-info (*get-file-info path)]
      (str "📄 文件: " (:name file-info) "\n"
           "📊 大小: " (:size file-info) " 字节\n\n"
           "📝 内容:\n\n" content))
    (str "❌ 文件不存在: " path)))

(defn- *read-text-file-lines
  "读取文本文件的指定行"
  [path start-line end-line]
  (try
    (with-open [rdr (io/reader path)]
      (let [lines (line-seq rdr)
            total-lines (count (vec lines))
            start (max 0 (dec start-line))  ;; 转为0-索引
            end (min total-lines (dec end-line))]  ;; 转为0-索引
        {:content (str/join "\n" (take (- end start) (drop start lines)))
         :total-lines total-lines
         :start-line start-line
         :end-line (min end-line total-lines)}))
    (catch Exception e
      (str "❌ 文件读取失败: " (.getMessage e)))))

(defn take-lines 
  ([path] (take-lines path 10))
  ([path n] 
   (if (*file-exists? path)
     (let [result (*read-text-file-lines path 1 n)]
       (if (map? result)
         (str "📄 文件: " (-> path io/file .getName) "\n"
              "📊 总行数: " (:total-lines result) "\n"
              "📝 前 " n " 行内容:\n\n" (:content result))
         result))
     (str "❌ 文件不存在: " path))))

(defn take-last-lines 
  ([path] (take-last-lines path 10))
  ([path n]
   (if (*file-exists? path)
     (try
       (with-open [rdr (io/reader path)]
         (let [lines (line-seq rdr)
               all-lines (vec lines)
               total-lines (count all-lines)
               start (max 0 (- total-lines n))
               content (str/join "\n" (drop start all-lines))]
           (str "📄 文件: " (-> path io/file .getName) "\n"
                "📊 总行数: " total-lines "\n"
                "📝 最后 " n " 行内容:\n\n" content)))
       (catch Exception e
         (str "❌ 文件读取失败: " (.getMessage e))))
     (str "❌ 文件不存在: " path))))


(defn take-lines-range [path start-line end-line]
  (if (*file-exists? path)
    (let [result (*read-text-file-lines path start-line end-line)]
      (if (map? result)
        (str "📄 文件: " (-> path io/file .getName) "\n"
             "📊 总行数: " (:total-lines result) "\n"
             "📝 第 " start-line " 至 " (:end-line result) " 行内容:\n\n"
             (:content result))
        result))
    (str "❌ 文件不存在: " path)))

(defn- *read-text-file-chunks
  "将文本文件分成多个块"
  [path chunk-size]
  (try
    (let [content (slurp path)
          total-length (count content)
          chunks (for [i (range 0 total-length chunk-size)]
                   (subs content i (min (+ i chunk-size) total-length)))]
      {:chunks chunks
       :total-chunks (count chunks)
       :total-length total-length})
    (catch Exception e
      (str "❌ 文件读取失败: " (.getMessage e)))))

 (defn read-text-file-chunks [path chunk-size]
  (if (*file-exists? path)
    (let [result (*read-text-file-chunks path chunk-size)]
      (if (map? result)
        (str/join "\n\n" 
                 (map-indexed
                  (fn [idx chunk]
                    (str "📄 文件: " (-> path io/file .getName) "\n"
                         "📊 总大小: " (:total-length result) " 字符\n"
                         "📦 分块数: " (:total-chunks result) "\n"
                         "📝 分块内容:\n\n"
                         "📦 块 #" (inc idx) ":\n" chunk))
                  (:chunks result)))
        result))
    (str "❌ 文件不存在: " path)))
 

 (defn read-json-file [path]
   (if (*file-exists? path)
     (try
       (let [content (slurp path)
             data (json/read-str content :key-fn keyword)]
         (str "📄 JSON文件: " (-> path io/file .getName) "\n"
              "📝 解析结果:\n\n"
              (with-out-str (clojure.pprint/pprint data))))
       (catch Exception e
         (str "❌ JSON解析失败: " (.getMessage e))))
     (str "❌ 文件不存在: " path)))

(defn read-edn-file [path]
  (if (*file-exists? path)
    (try
      (let [content (slurp path)
            data (edn/read-string content)]
        (str "📄 EDN文件: " (-> path io/file .getName) "\n"
             "📝 解析结果:\n\n"
             (with-out-str (clojure.pprint/pprint data))))
      (catch Exception e
        (str "❌ EDN解析失败: " (.getMessage e))))
    (str "❌ 文件不存在: " path)))

(defn read-csv-file [path]
  (if (*file-exists? path)
    (try
      (with-open [reader (io/reader path)]
        (let [data (csv/read-csv reader)
              headers (first data)
              rows (rest data)
              formatted (str "📄 CSV文件: " (-> path io/file .getName) "\n"
                             "📊 行数: " (inc (count rows)) "\n\n"
                             "📝 表头: " (str/join ", " headers) "\n\n"
                             "📝 数据 (前5行):\n")]
          (str formatted
               (str/join "\n"
                         (map #(str/join ", " %)
                              (take 5 rows))))))
      (catch Exception e
        (str "❌ CSV解析失败: " (.getMessage e))))
    (str "❌ 文件不存在: " path)))



 ;; ================ 增强搜索与统计================

 (defn search-text [path pattern]
   (if (*file-exists? path)
     (try
       (with-open [rdr (io/reader path)]
         (let [lines (line-seq rdr)
               matches (keep-indexed
                        (fn [idx line]
                          (when (str/includes? line pattern)
                            {:line (inc idx)
                             :content line}))
                        lines)]
           (if (seq matches)
             (str "🔍 搜索结果:\n\n"
                  (str/join "\n\n"
                            (map #(str "第 " (:line %) " 行:\n" (:content %))
                                 matches)))
             (str "❌ 未找到匹配内容"))))
       (catch Exception e
         (str "❌ 搜索失败: " (.getMessage e))))
     (str "❌ 文件不存在: " path)))
 

 (defn get-file-stats [path]
   (if (*file-exists? path)
     (try
       (let [content (slurp path)
                 lines (str/split-lines content)
                 chars (count content)
                 words (count (str/split (str/trim content) #"\s+"))
                 lines-count (count lines)]
             (str "📊 文件统计:\n\n"
                  "📝 字符数: " chars "\n"
                  "📝 单词数: " words "\n"
                  "📝 行数: " lines-count))
       (catch Exception e
         (str "❌ 统计失败: " (.getMessage e)))))
         (str "❌ 文件不存在: " path))

;; 文件写入函数
(defn write-text-file
  "将文本内容写入指定文件"
  [path content]
  (try
    (let [file (io/file path)]
      (io/make-parents file)
      (spit file content)
      (str "文件已成功写入: " path))
    (catch Exception e
      (str "写入文件失败: " (.getMessage e)))))

;; 文件更新函数
(defn update-text-file
  "更新指定文件的内容"
  [path content]
  (try
    (let [file (io/file path)]
      (if (.exists file)
        (do
          (spit file content)
          (str "文件已成功更新: " path))
        (str "文件不存在: " path)))
    (catch Exception e
      (str "更新文件失败: " (.getMessage e)))))

;; 文件移动函数
(defn move-file
  "移动文件到新位置"
  [source-path target-path]
  (try
    (let [source-file (io/file source-path)
          target-file (io/file target-path)]
      (if (.exists source-file)
        (do
          (io/make-parents target-file)
          (.renameTo source-file target-file)
          (str "文件已成功移动: " source-path " -> " target-path))
        (str "源文件不存在: " source-path)))
    (catch Exception e
      (str "移动文件失败: " (.getMessage e)))))

;; 文件删除函数
(defn delete-file
  "删除指定文件"
  [path]
  (try
    (let [file (io/file path)]
      (if (.exists file)
        (do
          (.delete file)
          (str "文件已成功删除: " path))
        (str "文件不存在: " path)))
    (catch Exception e
      (str "删除文件失败: " (.getMessage e)))))

;; 创建目录函数
(defn create-directory
  "创建目录"
  [path]
  (try
    (let [dir (io/file path)]
      (if (.exists dir)
        (str "目录已存在: " path)
        (do
          (.mkdirs dir)
          (str "目录已成功创建: " path))))
    (catch Exception e
      (str "创建目录失败: " (.getMessage e)))))

;; 文件搜索函数
(defn search-files
  "在指定目录中搜索匹配的文件"
  [dir-path pattern]
  (try
    (let [dir (io/file dir-path)
          files (file-seq dir)
          matching-files (filter #(and (.isFile %) 
                                       (re-find pattern (.getName %))) 
                                 files)]
      (str "找到 " (count matching-files) " 个匹配文件:\n"
           (str/join "\n" (map #(.getPath %) matching-files))))
    (catch Exception e
      (str "搜索文件失败: " (.getMessage e)))))

;; 文件内容搜索函数
(defn search-content
  "在指定目录中搜索包含特定内容的文件"
  [dir-path content-pattern]
  (try
    (let [dir (io/file dir-path)
          files (filter #(.isFile %) (file-seq dir))
          matching-files (filter #(try 
                                    (re-find content-pattern (slurp %))
                                    (catch Exception _ false)) 
                                 files)]
      (str "找到 " (count matching-files) " 个包含指定内容的文件:\n"
           (str/join "\n" (map #(.getPath %) matching-files))))
    (catch Exception e
      (str "搜索内容失败: " (.getMessage e)))))



