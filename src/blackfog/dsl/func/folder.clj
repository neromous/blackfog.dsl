(ns blackfog.dsl.func.folder
  (:require [clojure.java.io :as io]
            [clojure.string :as str]))

;; ================ 辅助函数 ================

(defn- *folder-exists?
  "检查文件夹是否存在"
  [path]
  (let [folder (io/file path)]
    (and (.exists folder) (.isDirectory folder))))

(defn- *get-folder-size
  "递归计算文件夹大小"
  [path]
  (let [folder (io/file path)]
    (if (and (.exists folder) (.isDirectory folder))
      (reduce + (map #(if (.isDirectory %)
                        (*get-folder-size (.getPath %))
                        (.length %))
                     (.listFiles folder)))
      0)))

;; ================ 文件夹操作函数 ================

(defn create-folder
  "创建文件夹"
  [path]
  (try
    (let [folder (io/file path)]
      (if (.exists folder)
        (str "文件夹已存在: " path)
        (do
          (.mkdirs folder)
          (str "文件夹已成功创建: " path))))
    (catch Exception e
      (str "创建文件夹失败: " (.getMessage e)))))

(defn delete-folder
  "删除文件夹及其内容"
  [path]
  (try
    (let [folder (io/file path)]
      (if (and (.exists folder) (.isDirectory folder))
        (let [delete-recursive (fn delete-recursive [file]
                                 (when (.isDirectory file)
                                   (run! delete-recursive (.listFiles file)))
                                 (.delete file))]
          (delete-recursive folder)
          (str "文件夹已成功删除: " path))
        (str "文件夹不存在: " path)))
    (catch Exception e
      (str "删除文件夹失败: " (.getMessage e)))))

(defn move-folder
  "移动文件夹到新位置"
  [source-path target-path]
  (try
    (let [source-folder (io/file source-path)
          target-folder (io/file target-path)]
      (if (and (.exists source-folder) (.isDirectory source-folder))
        (do
          (when-not (.exists (.getParentFile target-folder))
            (.mkdirs (.getParentFile target-folder)))
          (.renameTo source-folder target-folder)
          (str "文件夹已成功移动: " source-path " -> " target-path))
        (str "源文件夹不存在: " source-path)))
    (catch Exception e
      (str "移动文件夹失败: " (.getMessage e)))))

(defn copy-folder
  "复制文件夹及其内容到新位置"
  [source-path target-path]
  (try
    (let [source-folder (io/file source-path)
          target-folder (io/file target-path)]
      (if (and (.exists source-folder) (.isDirectory source-folder))
        (do
          (.mkdirs target-folder)
          (let [copy-recursive (fn copy-recursive [src dest]
                                 (if (.isDirectory src)
                                   (do
                                     (.mkdir dest)
                                     (doseq [file (.listFiles src)]
                                       (copy-recursive file (io/file dest (.getName file)))))
                                   (io/copy src dest)))]
            (copy-recursive source-folder target-folder)
            (str "文件夹已成功复制: " source-path " -> " target-path)))
        (str "源文件夹不存在: " source-path)))
    (catch Exception e
      (str "复制文件夹失败: " (.getMessage e)))))

(defn list-folders
  "列出指定路径下的所有文件夹"
  [path]
  (try
    (let [folder (io/file path)]
      (if (and (.exists folder) (.isDirectory folder))
        (let [folders (map #(.getPath %) 
                          (filter #(.isDirectory %) (.listFiles folder)))]
          (str "找到 " (count folders) " 个文件夹:\n"
               (str/join "\n" folders)))
        (str "文件夹不存在: " path)))
    (catch Exception e
      (str "列出文件夹失败: " (.getMessage e)))))

(defn search-folders
  "在指定路径下搜索匹配的文件夹"
  [path pattern]
  (try
    (let [folder (io/file path)]
      (if (and (.exists folder) (.isDirectory folder))
        (let [folders (map #(.getPath %) 
                          (filter #(and (.isDirectory %) 
                                        (re-find pattern (.getName %))) 
                                  (file-seq folder)))]
          (str "找到 " (count folders) " 个匹配的文件夹:\n"
               (str/join "\n" folders)))
        (str "文件夹不存在: " path)))
    (catch Exception e
      (str "搜索文件夹失败: " (.getMessage e)))))

(defn get-folder-size
  "获取文件夹大小（字节）"
  [path]
  (try
    (let [folder (io/file path)]
      (if (and (.exists folder) (.isDirectory folder))
        (let [size (*get-folder-size path)
              formatted (format "%.2f MB" (/ size 1048576.0))]
          (str "文件夹大小: " formatted " (" size " 字节)"))
        (str "文件夹不存在: " path)))
    (catch Exception e
      (str "获取文件夹大小失败: " (.getMessage e)))))

(defn get-folder-stats
  "获取文件夹统计信息"
  [path]
  (try
    (let [folder (io/file path)]
      (if (and (.exists folder) (.isDirectory folder))
        (let [all-files (file-seq folder)
              dirs (filter #(.isDirectory %) all-files)
              files (filter #(.isFile %) all-files)
              file-count (count files)
              dir-count (dec (count dirs)) ; 减去根目录自身
              total-size (reduce + (map #(.length %) files))
              extension-counts (frequencies (map #(last (str/split (.getName %) #"\.")) files))
              largest-files (take 5 (reverse (sort-by #(.length %) files)))]
          (str "文件夹统计信息:\n"
               "- 文件数: " file-count "\n"
               "- 目录数: " dir-count "\n"
               "- 总大小: " (format "%.2f MB" (/ total-size 1048576.0)) "\n"
               "- 最大的5个文件: \n  " 
               (str/join "\n  " (map #(str (.getName %) " (" 
                                           (format "%.2f KB" (/ (.length %) 1024.0)) ")") 
                                    largest-files))))
        (str "文件夹不存在: " path)))
    (catch Exception e
      (str "获取文件夹统计信息失败: " (.getMessage e)))))

(defn compare-folders
  "比较两个文件夹的内容"
  [path1 path2]
  (try
    (let [folder1 (io/file path1)
          folder2 (io/file path2)]
      (if (and (.exists folder1) (.isDirectory folder1)
               (.exists folder2) (.isDirectory folder2))
        (let [files1 (set (map #(.getPath %) (filter #(.isFile %) (file-seq folder1))))
              files2 (set (map #(.getPath %) (filter #(.isFile %) (file-seq folder2))))
              only-in-1 (clojure.set/difference files1 files2)
              only-in-2 (clojure.set/difference files2 files1)
              common (clojure.set/intersection files1 files2)]
          (str "文件夹比较结果:\n"
               "- 仅在第一个文件夹中: " (count only-in-1) " 个文件\n"
               "- 仅在第二个文件夹中: " (count only-in-2) " 个文件\n"
               "- 两个文件夹共有: " (count common) " 个文件"))
        (str "一个或两个文件夹不存在")))
    (catch Exception e
      (str "比较文件夹失败: " (.getMessage e)))))

(defn empty-folder
  "清空文件夹内容但保留文件夹本身"
  [path]
  (try
    (let [folder (io/file path)]
      (if (and (.exists folder) (.isDirectory folder))
        (let [contents (.listFiles folder)
              delete-recursive (fn delete-recursive [file]
                                 (if (.isDirectory file)
                                   (do
                                     (doseq [child-file (.listFiles file)]
                                       (delete-recursive child-file))
                                     (.delete file))
                                   (.delete file)))]
          ;; 对每个子文件/文件夹应用删除函数
          (doseq [item contents]
            (delete-recursive item))
          (str "文件夹已成功清空: " path))
        (str "文件夹不存在: " path)))
    (catch Exception e
      (str "清空文件夹失败: " (.getMessage e)))))

(defn folder-exists?
  "检查文件夹是否存在"
  [path]
  (let [exists (*folder-exists? path)]
    (if exists 
      (str "文件夹存在: " path) 
      (str "文件夹不存在: " path))))

(defn format-folder-tree
  "格式化文件夹树结构为可读文本"
  [path & [max-depth]]
  (try
    (let [folder (io/file path)
          max-depth (or max-depth 3)]
      (if (and (.exists folder) (.isDirectory folder))
        (let [format-tree
              (fn format-tree [file depth]
                (if (> depth max-depth)
                  ""
                  (let [indent (apply str (repeat (* 2 (dec depth)) " "))]
                    (if (.isDirectory file)
                      (let [children (.listFiles file)
                            dir-str (str indent "📁 " (.getName file) "\n")]
                        (if (seq children)
                          (str dir-str
                               (apply str (map #(format-tree % (inc depth))
                                               (sort-by #(.isFile %) children))))
                          (str dir-str)))
                      (str indent "📄 " (.getName file) "\n")))))]
          (str "文件夹树结构 (最大深度: " max-depth "):\n\n" (format-tree folder 1)))
        (str "文件夹不存在: " path)))
    (catch Exception e
      (str "格式化文件夹树失败: " (.getMessage e)))))