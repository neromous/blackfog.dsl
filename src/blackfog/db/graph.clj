(ns blackfog.db.graph
  "简易图数据库操作接口"
  (:require [datomic.api :as d]
            [blackfog.db.core :as db]
            [blackfog.utils.logger :as log]
            [clojure.spec.alpha :as s]))

;; ============ Specs ============

;; 基础类型
(s/def :phrase/id uuid?)
(s/def :phrase/title string?)
(s/def :phrase/content string?)
(s/def :phrase/domain (s/coll-of string?))
(s/def :phrase.related/words (s/coll-of string?))

;; 关系类型
(s/def :relation/type #{:is-a :has-a :related-to})

;; 短语实体
(s/def :phrase/entity
  (s/keys :req [:phrase/title]
          :opt [:phrase/id
                :phrase/content
                :phrase/domain
                :phrase.related/words]))

;; 返回结果
(s/def :result/success boolean?)
(s/def :result/error string?)
(s/def :db/operation-result
  (s/keys :req [:result/success]
          :opt [:result/error]))

;; ============ 短语操作 ============

(s/fdef create-phrase!
  :args (s/cat :phrase-map :phrase/entity)
  :ret :db/operation-result)

(defn create-phrase!
  "创建新短语
   参数: {:phrase/title \"标题\"
          :phrase/content \"描述\"
          :phrase/domain [\"领域1\" \"领域2\"]
          :phrase.related/words [\"相关词1\" \"相关词2\"]}
   返回: {:success true/false :result 结果}"
  [phrase-map]
  (db/create-entity! (assoc phrase-map
                            :phrase/id (java.util.UUID/randomUUID))))

(s/fdef find-phrase-by-title
  :args (s/cat :title string?)
  :ret (s/nilable :phrase/entity))

(defn find-phrase-by-title
  "根据标题查找短语"
  [title]
  (db/entity-map (db/find-unique :phrase/title title)))

(s/fdef find-phrases-by-domain
  :args (s/cat :domain string?)
  :ret (s/coll-of :phrase/entity))

(defn find-phrases-by-domain
  "根据领域查找短语"
  [domain]
  (->> (db/query '[:find ?e
                   :in $ ?domain
                   :where [?e :phrase/domain ?domain]]
                 [domain])
       (map first)
       (map db/entity-map)))

(s/fdef update-phrase!
  :args (s/cat :title string?
               :attrs (s/map-of keyword? any?))
  :ret (s/nilable :db/operation-result))

(defn update-phrase!
  "更新短语信息"
  [title attrs]
  (when-let [eid (db/find-unique :phrase/title title)]
    (db/update-entity! eid attrs)))

(s/fdef delete-phrase!
  :args (s/cat :title string?)
  :ret (s/nilable :db/operation-result))

(defn delete-phrase!
  "删除短语"
  [title]
  (when-let [eid (db/find-unique :phrase/title title)]
    (db/retract-entity! eid)))

(s/fdef add-relation!
  :args (s/cat :from-title string?
               :to-title string?
               :relation-type :relation/type)
  :ret (s/nilable :db/operation-result))

(defn add-relation!
  "添加短语间的关系
   例如: (add-relation! \"短语A\" \"短语B\" :is-a)"
  [from-title to-title relation-type]
  (when-let [from-id (db/find-unique :phrase/title from-title)]
    (when-let [to-id (db/find-unique :phrase/title to-title)]
      (db/update-entity! from-id
                         {:phrase/relation #{{:relation/type relation-type
                                              :db/id to-id}}}))))

(s/def :related-phrase/result
  (s/keys :req-un [:phrase/entity :relation/type]))

(s/fdef find-related-phrases
  :args (s/cat :title string?)
  :ret (s/nilable (s/coll-of :related-phrase/result)))

(defn find-related-phrases
  "查找与指定短语相关的所有短语"
  [title]
  (when-let [eid (db/find-unique :phrase/title title)]
    (->> (db/query '[:find ?rel ?type
                     :in $ ?e1
                     :where
                     [?e1 :phrase/relation ?rel]
                     [?rel :relation/type ?type]]
                   [eid])
         (map (fn [[e2 type]]
                {:phrase (db/entity-map e2)
                 :relation type})))))

;; ============ 示例用法 ============
(comment
  ;; 创建短语
  (create-phrase! {:phrase/title "函数式编程"
                   :phrase/content "一种编程范式"
                   :phrase/domain ["编程" "计算机科学"]
                   :phrase.related/words ["lambda" "纯函数"]})

  ;; 添加关系
  (add-relation! "函数式编程" "编程范式" :is-a)

  ;; 查找相关短语
  (find-related-phrases "函数式编程"))

;; ============ 开发时启用 spec 检查 ============
(comment
  (s/check-asserts true)

  (add-relation! "函数式编程" "编程范式" :is-a)

  (find-related-phrases "函数式编程")

;; 先创建两个短语
  (create-phrase! {:phrase/title "函数式编程"
                   :phrase/content "一种编程范式"
                   :phrase/domain ["编程" "计算机科学"]})

  (create-phrase! {:phrase/title "编程范式"
                   :phrase/content "编程的基本风格和方法"
                   :phrase/domain ["编程" "计算机科学"]})

;; 添加关系
  (add-relation! "函数式编程" "编程范式" :is-a)

;; 查询关系
  (find-related-phrases "函数式编程"))
