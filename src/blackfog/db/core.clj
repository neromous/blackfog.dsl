(ns blackfog.db.core
  "Core database operations and connection management"
  (:require [datomic.api :as d]
            [clojure.java.io :as io]
            [clojure.edn :as edn]
            [blackfog.utils.logger :as log]))

;; ============ 配置管理 ============do

(def ^:private default-config
  {:uri "datomic:dev://localhost:4334/blackfog-dev"
   :schema-path "schema/base.edn"
   :schema/graph "schema/graph.edn"})

(def ^:private config-atom (atom default-config))

(defn set-config!
  "设置数据库配置。接受一个包含配置选项的映射。
   可选键:
   - :uri - Datomic 数据库 URI
   - :schema-path - schema EDN 文件的资源路径"
  [config-map]
  (swap! config-atom merge config-map))

(defn get-config
  "获取当前数据库配置"
  []
  @config-atom)

(defn- db-uri
  "获取当前数据库 URI"
  []
  (:uri (get-config)))

;; ============ 连接管理 ============

(defn init-db
  "初始化数据库并加载基础 schema。
   成功时返回数据库连接。
   失败时抛出带有详细信息的异常。"
  []
  (try
    (let [uri (db-uri)]
      (when-not (d/create-database uri)
        (log/info "数据库已存在:" uri))

      (let [conn (d/connect uri)
            schema-path (:schema-path (get-config))
            graph-schema-path (:schema/graph (get-config))
            schema (-> schema-path
                       io/resource
                       slurp
                       edn/read-string)
            graph-schema (-> graph-schema-path
                             io/resource
                             slurp
                             edn/read-string)
            schema (into schema graph-schema)]
        (log/info "正在初始化数据库 schema...")
        @(d/transact conn schema)
        (log/info "Schema 初始化完成")
        conn))
    (catch Exception e
      (log/error "数据库初始化失败:" (ex-message e))
      (throw (ex-info "数据库初始化失败"
                      {:uri (db-uri)
                       :cause (ex-message e)}
                      e)))))

(def ^:private conn-atom (atom nil))

(defn get-conn
  "获取数据库连接。如果连接不存在或已关闭，则初始化新连接。"
  []
  (try 
    (when (nil? @conn-atom)
      (reset! conn-atom (init-db)))
    ;; 简单测试连接是否有效
    (d/db @conn-atom)
    @conn-atom
    (catch Exception _
      (reset! conn-atom (init-db))
      @conn-atom)))

(defn get-db
  "获取当前数据库值。确保连接已初始化。"
  []
  (d/db (get-conn)))

(defn release-conn!
  "释放数据库连接资源"
  []
  (when-let [conn @conn-atom]
    (try
      (.close ^java.io.Closeable conn)
      (reset! conn-atom nil)
      (log/info "数据库连接已关闭")
      (catch Exception e
        (log/warn "关闭数据库连接时出错:" (ex-message e))))))

;; ============ 事务操作 ============

(defn transact!
  "数据库事务操作的包装函数，带有错误处理。
   成功时返回 {:success true, :result tx-result}，
   失败时返回 {:success false, :error error-message}。"
  [tx-data]
  (try
    (let [result @(d/transact (get-conn) tx-data)]
      {:success true
       :result result})
    (catch Exception e
      (log/error "事务执行失败:" (ex-message e))
      {:success false
       :error (ex-message e)})))

;; ============ 查询操作 ============
(defn query
  "执行 Datalog 查询。
   参数:
   - query-map: Datalog 查询 (必需)
   - inputs: 查询的额外输入 (可选)
   
   返回查询结果集。"
  ([query-map]
   (query query-map []))
  ([query-map inputs]
   (try
     (apply d/q query-map (get-db) inputs)
     (catch Exception e
       (log/error "查询执行失败:" (ex-message e))
       (throw (ex-info "查询执行失败"
                       {:query query-map
                        :inputs inputs
                        :cause (ex-message e)}
                       e))))))

(defn entity
  "根据实体 ID 获取实体。返回 datomic 实体对象。"
  [entity-id]
  (d/entity (get-db) entity-id))

(defn entity-map
  "根据实体 ID 获取实体，并将其转换为普通的 Clojure 映射。"
  [entity-id]
  (when-let [e (entity entity-id)]
    (into {} e)))

(defn find-by-attr
  "根据属性值查找实体。
   参数:
   - attr: 要查询的属性
   - value: 属性值
   
   返回匹配的实体 ID 列表。"
  [attr value]
  (map first (query '[:find ?e :in $ ?attr ?value :where [?e ?attr ?value]]
                    [attr value])))

(defn find-unique
  "根据唯一属性查找单个实体。
   参数:
   - attr: 唯一属性
   - value: 属性值
   
   返回实体 ID 或 nil。"
  [attr value]
  (first (find-by-attr attr value)))

;; ============ 实体操作 ============

(defn create-entity!
  "创建新实体。
   参数:
   - entity-map: 实体属性映射
   
   返回事务结果，包含新创建的实体 ID。"
  [entity-map]
  (transact! [(assoc entity-map :db/id #db/id[:db.part/user])]))

(defn update-entity!
  "更新现有实体。
   参数:
   - entity-id: 要更新的实体 ID
   - attrs: 要更新的属性映射
   
   返回事务结果。"
  [entity-id attrs]
  (transact! [(assoc attrs :db/id entity-id)]))

(defn retract-entity!
  "删除实体及其所有属性。
   参数:
   - entity-id: 要删除的实体 ID
   
   返回事务结果。"
  [entity-id]
  (transact! [[:db.fn/retractEntity entity-id]]))

(defn retract-attr!
  "删除实体的特定属性。
   参数:
   - entity-id: 实体 ID
   - attr: 要删除的属性
   
   返回事务结果。"
  [entity-id attr]
  (when-let [e (entity entity-id)]
    (when-let [v (attr e)]
      (transact! [[:db/retract entity-id attr v]]))))

;; ============ 历史查询 ============

(defn history-db
  "获取数据库的历史视图"
  []
  (d/history (get-db)))

(defn entity-history
  "获取实体的历史变更。
   参数:
   - entity-id: 实体 ID
   
   返回实体历史变更的列表，按时间排序。"
  [entity-id]
  (query '[:find ?tx ?attr ?val ?added ?inst
           :in $ ?e
           :where
           [?e ?attr ?val ?tx ?added]
           [?tx :db/txInstant ?inst]]
         [(history-db) entity-id]))

;; ============ 示例用法 ============
(comment
  ;; 配置数据库
  (set-config! {:uri "datomic:free://localhost:4334/blackfog-prod"})

  ;; 初始化数据库
  (get-conn)

  ;; 创建用户
  (create-entity! {:user/id (java.util.UUID/randomUUID)
                   :user/name "张三"
                   :user/email "zhangsan@example.com"
                   :user/created-at (java.util.Date.)})

  ;; 查询用户
  (query '[:find ?e ?name ?email
           :where
           [?e :user/name ?name]
           [?e :user/email ?email]])

  ;; 根据邮箱查找用户
  (find-unique :user/email "zhangsan@example.com")

  ;; 更新用户
  (let [user-id (find-unique :user/email "zhangsan@example.com")]
    (update-entity! user-id {:user/name "张三丰"}))

  ;; 获取实体历史
  (let [user-id (find-unique :user/email "zhangsan@example.com")]
    (entity-history user-id))

  ;; 关闭连接
  (release-conn!))



