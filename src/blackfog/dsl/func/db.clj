(ns blackfog.dsl.func.db
  "注册图数据库操作到 DSL 系统"
  (:require [blackfog.db.graph :as graph]
            [clojure.string :as str]
            [clojure.pprint :as pp]))

;; 辅助函数：将数据转换为易读的文本
(defn- format-result [result]
  (cond
    (map? result) (with-out-str (pp/pprint result))
    (coll? result) (str/join "\n" (map #(with-out-str (pp/pprint %)) result))
    :else (str result)))

;; 将短语转换为易读的图标风格文本
(defn- phrase-to-easy-read
  "将短语数据转换为易读的图标风格文本"
  [phrase]
  (when phrase
    (let [title (:phrase/title phrase)
          content (:phrase/content phrase)
          domain (:phrase/domain phrase)
          related-words (:phrase.related/words phrase)]
      (str/join "\n"
                (filter some?
                        [(when title (str "📚 " title))
                         (when content (str "\n   " content))
                         (when (seq domain)
                           (str "\n\n🏷️ 领域：" (str/join ", " domain)))
                         (when (seq related-words)
                           (str "\n\n🔄 相关词：" (str/join ", " related-words)))
                         "\n"])))))

;; 将关系转换为易读的图标风格文本
(defn- relations-to-easy-read
  "将关系数据转换为易读的图标风格文本"
  [title relations]
  (when (seq relations)
    (str/join "\n"
              (concat
               [(str "🔗 " title " 的关系")]
               (for [rel relations]
                 (let [rel-type (:relation rel)
                       target (get-in rel [:phrase :phrase/title])
                       content (get-in rel [:phrase :phrase/content])]
                   (case rel-type
                     :is-a (str "   ⬆️ 是一种 " target)
                     :has-a (str "   ⬇️ 包含 " target)
                     :related-to (str "   ↔️ 相关于 " target)
                     (str "   ➡️ " (name rel-type) " " target))))))))

;; 将领域知识转换为易读的图标风格文本
(defn- domain-to-easy-read
  "将领域知识转换为易读的图标风格文本"
  [domain phrases relations]
  (str/join "\n\n"
            [(str "🌐 领域：" domain)
             (str "   📊 统计：" (count phrases) " 个短语，" (count relations) " 个关系")
             (str "📚 短语列表")
             (str/join "\n\n"
                       (for [phrase phrases]
                         (str "   📝 " (:phrase/title phrase)
                              "\n   " (or (:phrase/content phrase) "无描述")
                              (when (seq (:phrase.related/words phrase))
                                (str "\n   🔄 相关词：" (str/join ", " (:phrase.related/words phrase)))))))
             (when (seq relations)
               (str "🔗 关系网络\n"
                    (str/join "\n"
                              (for [rel relations]
                                (let [source (:source rel)
                                      rel-type (:relation rel)
                                      target (get-in rel [:phrase :phrase/title])]
                                  (case rel-type
                                    :is-a (str "   " source " ⬆️ 是一种 " target)
                                    :has-a (str "   " source " ⬇️ 包含 " target)
                                    :related-to (str "   " source " ↔️ 相关于 " target)
                                    (str "   " source " ➡️ " (name rel-type) " " target)))))))]))

(defn create-phrase [phrase-map]
  (let [result (graph/create-phrase! phrase-map)]
    (if (:success result)
      (str "✅ 短语创建成功: " (:phrase/title phrase-map))
      (str "❌ 短语创建失败: " (:error result)))))

(defn find-phrase [title]
  (if-let [phrase (graph/find-phrase-by-title title)]
    (phrase-to-easy-read phrase)
    (str "❓ 未找到短语: " title)))

(defn find-by-domain [domain]
  (let [phrases (graph/find-phrases-by-domain domain)]
    (if (seq phrases)
      (str "# 领域：" domain "\n\n"
           (str/join "\n\n" (map phrase-to-easy-read phrases)))
      (str "❓ 在领域 '" domain "' 中未找到短语"))))


(defn update-phrase [title attrs]
  (let [result (graph/update-phrase! title attrs)]
    (if (and result (:success result))
      (str "✅ 短语 '" title "' 更新成功")
      (str "❌ 短语 '" title "' 更新失败"))))

(defn delete-phrase [title]
  (let [result (graph/delete-phrase! title)]
    (if (and result (:success result))
      (str "✅ 短语 '" title "' 删除成功")
      (str "❌ 短语 '" title "' 删除失败"))))

(defn add-relation [from-title to-title relation-type]
  (let [result (graph/add-relation! from-title to-title relation-type)]
    (if (and result (:success result))
      (str "✅ 关系添加成功: " from-title " " relation-type " " to-title)
      (str "❌ 关系添加失败: " from-title " " relation-type " " to-title))))

(defn find-relations [title]
  (if-let [relations (graph/find-related-phrases title)]
    (relations-to-easy-read title relations)
    (str "❓ 未找到与 '" title "' 相关的短语")))

(defn create-with-relation [phrase-map related-to relation-type]
  (let [title (:phrase/title phrase-map)
        create-result (graph/create-phrase! phrase-map)]
    (if (:success create-result)
      (let [add-result (graph/add-relation! title (:phrase/title related-to) relation-type)]
        (if (:success add-result)
          (str "✅ 短语创建成功: " title "\n"
               "   🔗 关系添加成功: " title " " relation-type " " (:phrase/title related-to))
          (str "❌ 关系添加失败: " title " " relation-type " " (:phrase/title related-to))))
      (str "❌ 短语创建失败: " title))))

(defn domain-knowledge [domain]
  (let [phrases (graph/find-phrases-by-domain domain)
        phrase-relations (mapcat
                          (fn [phrase]
                            (let [title (:phrase/title phrase)
                                  relations (graph/find-related-phrases title)]
                              (map #(assoc % :source title) relations)))
                          phrases)]
    (if (seq phrases)
      (domain-to-easy-read domain phrases phrase-relations)
      (str "❓ 在领域 '" domain "' 中未找到短语"))))


(defn visualize-domain [domain]
 (let [phrases (graph/find-phrases-by-domain domain)
      relations (mapcat
                 (fn [phrase]
                   (let [title (:phrase/title phrase)
                         rels (graph/find-related-phrases title)]
                     (map (fn [rel]
                            {:from title
                             :type (:relation rel)
                             :to (get-in rel [:phrase :phrase/title])})
                          rels)))
                 phrases)]
  (str "🌐 " domain " 知识图谱\n\n"
       "📚 节点列表\n\n"
       (str/join "\n"
                 (map-indexed
                  (fn [idx phrase]
                    (str "   " (inc idx) ". " (:phrase/title phrase)))
                  phrases))
       "\n\n🔗 关系列表\n\n"
       (if (seq relations)
         (str/join "\n"
                   (map (fn [rel]
                          (case (:type rel)
                            :is-a (str "   " (:from rel) " ⬆️ 是一种 " (:to rel))
                            :has-a (str "   " (:from rel) " ⬇️ 包含 " (:to rel))
                            :related-to (str "   " (:from rel) " ↔️ 相关于 " (:to rel))
                            (str "   " (:from rel) " ➡️ " (name (:type rel)) " " (:to rel))))
                        relations))
         "   没有找到关系"))))
