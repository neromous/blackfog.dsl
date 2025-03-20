# 简易图数据库使用指南

## 什么是图数据库？

图数据库是一种存储数据的方式，它将数据表示为"节点"和"边"：
- **节点**：代表实体，如人物、地点、事物等
- **边**：代表节点之间的关系，如"认识"、"位于"、"拥有"等

## 三分钟上手指南

### 1. 基本概念

我们的图数据库只有两种元素：
- **节点(Node)**：存储实体信息
- **边(Edge)**：连接两个节点，表示它们之间的关系

### 2. 创建节点

```clojure
;; 创建一个人物节点
(d/transact conn [{:node/id (d/squuid)
                   :node/label "人物"
                   :node/properties {:name "张三" :age 30}
                   :node/created-at (java.util.Date.)}])
```

### 3. 创建关系（边）

```clojure
;; 创建一个"认识"关系（张三认识李四）
(d/transact conn [{:edge/id (d/squuid)
                   :edge/from zhang-san-id  ;; 起始节点ID
                   :edge/to li-si-id        ;; 目标节点ID
                   :edge/label "认识"
                   :edge/properties {:years 5}  ;; 认识了5年
                   :edge/created-at (java.util.Date.)}])
```

### 4. 查询示例

查找张三认识的所有人：

```clojure
(d/q '[:find ?friend-name
       :where
       [?p :node/label "人物"]
       [?p :node/properties ?p-props]
       [(get ?p-props :name) ?p-name]
       [(= ?p-name "张三")]
       [?e :edge/from ?p]
       [?e :edge/label "认识"]
       [?e :edge/to ?friend]
       [?friend :node/properties ?f-props]
       [(get ?f-props :name) ?friend-name]]
     db)
```

## 节点属性

每个节点包含以下属性：
- `:node/id`：唯一标识符（UUID）
- `:node/label`：节点类型标签（如"人物"、"地点"等）
- `:node/properties`：节点属性（存储为map）
- `:node/created-at`：创建时间

## 边属性

每个边包含以下属性：
- `:edge/id`：唯一标识符（UUID）
- `:edge/from`：起始节点引用
- `:edge/to`：目标节点引用
- `:edge/label`：关系类型（如"认识"、"位于"等）
- `:edge/properties`：边的属性（存储为map）
- `:edge/weight`：关系权重（可选）
- `:edge/created-at`：创建时间

## 完整示例

请查看 `resources/examples/graph_examples.clj` 文件，其中包含完整的使用示例。 