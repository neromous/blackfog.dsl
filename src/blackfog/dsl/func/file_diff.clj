(ns blackfog.dsl.func.file-diff
  (:require [clojure.string :as str]
            [clojure.java.io :as io])
  (:import [java.io File]
           [com.github.difflib DiffUtils]
           [com.github.difflib.algorithm.myers MyersDiff]
           [com.github.difflib.text DiffRowGenerator]
           [java.util ArrayList]))

(declare parse-inline-diff)

;; 辅助函数：将文本转换为行列表
(defn- text->lines
  "将文本字符串转换为行列表"
  [text]
  (str/split-lines text))

;; 辅助函数：从文件读取文本
(defn- file->lines
  "从文件读取文本并转换为行列表"
  [file-path]
  (with-open [rdr (io/reader file-path)]
    (doall (line-seq rdr))))

(defn compute-diff
  "计算两个文本之间的差异，返回结构化的差异数据"
  [original-text modified-text]
  (let [original-lines (text->lines original-text)
        modified-lines (text->lines modified-text)
        ;; 使用java-diff-utils计算差异
        diff-result (DiffUtils/diff original-lines modified-lines (MyersDiff.))
        deltas (.getDeltas diff-result)

        ;; 创建行内差异生成器
        row-gen (let [tag-fn (fn [s] (str "[" s "]"))
                      old-tag-fn (reify java.util.function.Function
                                   (apply [_ s] (tag-fn s)))
                      new-tag-fn (reify java.util.function.Function
                                   (apply [_ s] (tag-fn s)))]
                  (-> (DiffRowGenerator/create)
                      (.showInlineDiffs true)
                      (.inlineDiffBySplitter true)
                      (.oldTag old-tag-fn)
                      (.newTag new-tag-fn)
                      (.build)))

        ;; 处理差异结果
        changes (atom [])
        current-orig-line (atom 0)
        current-new-line (atom 0)]

    ;; 处理每个差异块
    (doseq [delta deltas]
      (let [position (.getPosition delta)
            orig-start position
            orig-end (+ position (.size (.getSource delta)))
            new-start position
            new-end (+ position (.size (.getTarget delta)))

            ;; 添加未变更的行
            _ (doseq [i (range @current-orig-line orig-start)]
                (let [line (nth original-lines i)]
                  (swap! changes conj {:type :equal
                                       :original-line (inc i)
                                       :new-line (inc @current-new-line)
                                       :content line})
                  (swap! current-new-line inc)))

            ;; 更新当前行位置
            _ (reset! current-orig-line orig-start)

            ;; 获取原始和修改后的行
            orig-lines (vec (take (.size (.getSource delta))
                                  (drop position original-lines)))
            new-lines (vec (take (.size (.getTarget delta))
                                 (drop position modified-lines)))

            ;; 根据差异类型处理
            delta-type (.getType delta)]

        (case (keyword (str/lower-case (str delta-type)))
          :insert
          (doseq [i (range (count new-lines))]
            (swap! changes conj {:type :insert
                                 :original-line nil
                                 :new-line (inc (+ new-start i))
                                 :content (nth new-lines i)})
            (swap! current-new-line inc))

          :delete
          (doseq [i (range (count orig-lines))]
            (swap! changes conj {:type :delete
                                 :original-line (inc (+ orig-start i))
                                 :new-line nil
                                 :content (nth orig-lines i)})
            (swap! current-orig-line inc))

          :change
          (if (and (= (count orig-lines) 1) (= (count new-lines) 1))
            ;; 单行变更 - 计算行内差异
            (let [orig-line (first orig-lines)
                  new-line (first new-lines)
                  diff-rows (.generateDiffRows row-gen
                                               (doto (ArrayList.) (.add orig-line))
                                               (doto (ArrayList.) (.add new-line)))
                  diff-row (first diff-rows)
                  old-line (.getOldLine diff-row)
                  new-line (.getNewLine diff-row)
                  ;; 解析行内差异
                  inline-diff (parse-inline-diff old-line new-line)]
              (swap! changes conj {:type :change
                                   :original-line (inc orig-start)
                                   :new-line (inc new-start)
                                   :original-content orig-line
                                   :new-content new-line
                                   :diff inline-diff})
              (swap! current-orig-line inc)
              (swap! current-new-line inc))

            ;; 多行变更 - 作为删除+插入处理
            (do
              (doseq [i (range (count orig-lines))]
                (swap! changes conj {:type :delete
                                     :original-line (inc (+ orig-start i))
                                     :new-line nil
                                     :content (nth orig-lines i)})
                (swap! current-orig-line inc))
              (doseq [i (range (count new-lines))]
                (swap! changes conj {:type :insert
                                     :original-line nil
                                     :new-line (inc (+ new-start i))
                                     :content (nth new-lines i)})
                (swap! current-new-line inc))))))

    ;; 添加剩余未变更的行
      (doseq [i (range @current-orig-line (count original-lines))]
        (let [line (nth original-lines i)]
          (swap! changes conj {:type :equal
                               :original-line (inc i)
                               :new-line (inc @current-new-line)
                               :content line})
          (swap! current-new-line inc)))

    ;; 返回结构化差异结果
      {:changes @changes})))

;; 辅助函数：解析行内差异
(defn- parse-inline-diff
  "解析行内差异，提取变更部分"
  [old-line new-line]
  (let [parse-tags (fn [line]
                     (loop [result []
                            text line
                            in-tag? false
                            current ""]
                       (if (empty? text)
                         (if (empty? current)
                           result
                           (conj result {:type :equal :text current}))
                         (let [ch (first text)
                               rest-text (subs text 1)]
                           (cond
                             ;; 标签开始
                             (and (= ch \[) (not in-tag?))
                             (recur (if (empty? current)
                                      result
                                      (conj result {:type :equal :text current}))
                                    rest-text
                                    true
                                    "")

                             ;; 标签结束
                             (and (= ch \]) in-tag?)
                             (recur (conj result {:type :tag :text current})
                                    rest-text
                                    false
                                    "")

                             ;; 标签内容或普通文本
                             :else
                             (recur result
                                    rest-text
                                    in-tag?
                                    (str current ch)))))))]

    ;; 解析标签并合并结果
    (let [old-parts (parse-tags old-line)
          new-parts (parse-tags new-line)
          old-tags (filter #(= (:type %) :tag) old-parts)
          new-tags (filter #(= (:type %) :tag) new-parts)
          result (atom [])]

      ;; 提取相等部分和变更部分
      (loop [o-parts old-parts
             n-parts new-parts]
        (cond
          ;; 两边都处理完毕
          (and (empty? o-parts) (empty? n-parts))
          nil

          ;; 处理标签
          (and (seq o-parts) (seq n-parts)
               (= (:type (first o-parts)) :tag)
               (= (:type (first n-parts)) :tag))
          (do
            (swap! result conj {:type :change
                                :old (:text (first o-parts))
                                :new (:text (first n-parts))})
            (recur (rest o-parts) (rest n-parts)))

          ;; 处理相等部分
          (and (seq o-parts) (seq n-parts)
               (= (:type (first o-parts)) :equal)
               (= (:type (first n-parts)) :equal)
               (= (:text (first o-parts)) (:text (first n-parts))))
          (do
            (swap! result conj {:type :equal
                                :text (:text (first o-parts))})
            (recur (rest o-parts) (rest n-parts)))

          ;; 其他情况（不应该发生）
          :else
          (if (seq o-parts)
            (recur (rest o-parts) n-parts)
            (recur o-parts (rest n-parts)))))

      @result)))

;; 生成可视化差异输出
(defn generate-visual-diff
  "生成带行号的可视化差异输出"
  [diff-result]
  (let [changes (:changes diff-result)
        lines (for [change changes]
                (case (:type change)
                  :equal
                  (format "%3d | %3d | %s"
                          (:original-line change)
                          (:new-line change)
                          (:content change))

                  :insert
                  (format "    | %3d |+%s"
                          (:new-line change)
                          (:content change))

                  :delete
                  (format "%3d |     |-%s"
                          (:original-line change)
                          (:content change))

                  :change
                  [(format "%3d | %3d |-%s"
                           (:original-line change)
                           (:new-line change)
                           (apply str
                                  (for [part (:diff change)]
                                    (if (= (:type part) :equal)
                                      (:text part)
                                      (if (:old part)
                                        (str "[" (:old part) "]")
                                        "")))))
                   (format "    |     |+%s"
                           (apply str
                                  (for [part (:diff change)]
                                    (if (= (:type part) :equal)
                                      (:text part)
                                      (if (:new part)
                                        (str "[" (:new part) "]")
                                        "")))))]))]
    (str/join "\n" (flatten lines))))

;; 主API函数 - 字符串输入
(defn diff-text
  "比较两个文本字符串，返回差异结果"
  [original-text modified-text]
  (let [diff-result (compute-diff original-text modified-text)]
    {:structured diff-result
     :visual (generate-visual-diff diff-result)}))

;; 主API函数 - 文件输入
(defn diff-files
  "比较两个文件，返回差异结果"
  [original-file modified-file]
  (let [original-text (slurp original-file)
        modified-text (slurp modified-file)]
    (diff-text original-text modified-text)))

;; 示例用法
(comment
  ;; 示例文本
  (def original-text "这是第一行内容。
这是第二行内容。
这是第三行内容。
这是第四行内容。
这是将被删除的第五行。
这是第六行内容。
这是第七行，将有小幅修改。
这是第八行内容。
这是第九行内容，将有较大修改。
这是第十行内容。")

  (def modified-text "这是第一行内容。
这是第二行内容。
这是第三行内容。
这是第四行内容。
这是新插入的第五行。
这是第六行内容。
这是第七行，已经小幅修改。
这是第八行内容。
这是第九行内容，已经完全被重写了。
这是第十行内容。
这是新增的第十一行。")

  ;; 计算差异
  (def result (diff-text original-text modified-text))

  ;; 查看结构化结果
  (:structured result)

  ;; 查看可视化结果
  (println (:visual result)))

