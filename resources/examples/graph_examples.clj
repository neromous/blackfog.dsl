(ns examples.graph-examples
  (:require [datomic.api :as d]
            [clojure.pprint :as pp]))

;; ===== 图数据库使用示例 =====
;; 这个文件展示了如何使用简易图数据库进行基本操作

;; 假设我们已经有了一个连接
;; (def conn (d/connect "datomic:dev://localhost:4334/graph-db"))

;; ===== 1. 创建节点 =====

;; 创建人物节点
(defn create-person! [conn name age]
  (let [node-id (d/squuid)]
    @(d/transact conn [{:node/id node-id
                        :node/label "人物"
                        :node/properties {:name name :age age}
                        :node/created-at (java.util.Date.)}])
    node-id))

;; 创建地点节点
(defn create-place! [conn name location]
  (let [node-id (d/squuid)]
    @(d/transact conn [{:node/id node-id
                        :node/label "地点"
                        :node/properties {:name name :location location}
                        :node/created-at (java.util.Date.)}])
    node-id))

;; ===== 2. 创建边（关系）=====

;; 创建"认识"关系
(defn create-knows-relation! [conn person1-id person2-id years]
  (let [edge-id (d/squuid)]
    @(d/transact conn [{:edge/id edge-id
                        :edge/from person1-id
                        :edge/to person2-id
                        :edge/label "认识"
                        :edge/properties {:years years}
                        :edge/weight (float (/ 1 years)) ;; 认识时间越短，关系越紧密
                        :edge/created-at (java.util.Date.)}])
    edge-id))

;; 创建"位于"关系
(defn create-located-at-relation! [conn person-id place-id]
  (let [edge-id (d/squuid)]
    @(d/transact conn [{:edge/id edge-id
                        :edge/from person-id
                        :edge/to place-id
                        :edge/label "位于"
                        :edge/created-at (java.util.Date.)}])
    edge-id))

;; ===== 3. 查询示例 =====

;; 查找某人认识的所有人
(defn find-all-friends [db person-name]
  (d/q '[:find ?friend-name ?years
         :in $ ?name
         :where
         [?p :node/label "人物"]
         [?p :node/properties ?p-props]
         [(get ?p-props :name) ?p-name]
         [(= ?p-name ?name)]
         [?e :edge/from ?p]
         [?e :edge/label "认识"]
         [?e :edge/properties ?e-props]
         [(get ?e-props :years) ?years]
         [?e :edge/to ?friend]
         [?friend :node/properties ?f-props]
         [(get ?f-props :name) ?friend-name]]
       db person-name))

;; 查找某地点的所有人
(defn find-people-at-place [db place-name]
  (d/q '[:find ?person-name ?age
         :in $ ?place-name
         :where
         [?place :node/label "地点"]
         [?place :node/properties ?place-props]
         [(get ?place-props :name) ?p-name]
         [(= ?p-name ?place-name)]
         [?e :edge/to ?place]
         [?e :edge/label "位于"]
         [?e :edge/from ?person]
         [?person :node/properties ?person-props]
         [(get ?person-props :name) ?person-name]
         [(get ?person-props :age) ?age]]
       db place-name))

;; ===== 4. 使用示例 =====

