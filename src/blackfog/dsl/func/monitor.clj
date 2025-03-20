(ns blackfog.dsl.func.monitor
  (:require [blackfog.dsl.core :refer [reg-element]]
            [blackfog.monitor.core :as monitor-core]
            [blackfog.monitor.registry :as registry]
            [blackfog.monitor.supervisor :as supervisor]))

;; 全局监控管理器实例
(def ^:private global-supervisor (atom nil))

;; 确保监控管理器已初始化
(defn- ensure-supervisor! 
  "确保全局监控管理器已初始化"
  [event-handler]
  (when (nil? @global-supervisor)
    (reset! global-supervisor (supervisor/create-monitor-supervisor event-handler))))

;; 默认事件处理器
(defn- default-event-handler 
  "默认的事件处理器，简单打印事件信息"
  [event]
  (println "收到监控事件:" (:event-id event) 
           "来源:" (:source-type event) 
           "类型:" (:content-type event)))

;; 初始化监控系统
(defn init-monitor-system! 
  "初始化监控系统，可选自定义事件处理器"
  ([] (init-monitor-system! default-event-handler))
  ([event-handler]
   (when @global-supervisor
     (supervisor/shutdown-supervisor! @global-supervisor))
   (reset! global-supervisor (supervisor/create-monitor-supervisor event-handler))
   {:status :initialized}))

;; 关闭监控系统
(defn shutdown-monitor-system! 
  "关闭监控系统"
  []
  (when @global-supervisor
    (supervisor/shutdown-supervisor! @global-supervisor)
    (reset! global-supervisor nil)
    {:status :shutdown}))

;; 注册DSL函数

;; 添加监控源
(reg-element :monitor/add
  (fn [id type config]
    (ensure-supervisor! default-event-handler)
    ((:add-monitor @global-supervisor) id type config)
    {:monitor-id id
     :type type
     :status :started}))

;; 移除监控源
(reg-element :monitor/remove
  (fn [id]
    (when @global-supervisor
      ((:remove-monitor @global-supervisor) id)
      {:monitor-id id
       :status :removed})))

;; 列出所有监控源
(reg-element :monitor/list
  (fn []
    (if @global-supervisor
      {:monitors ((:list-monitors @global-supervisor))}
      {:monitors []})))

;; 获取监控源状态
(reg-element :monitor/status
  (fn [id]
    (if @global-supervisor
      {:monitor-id id
       :status ((:get-status @global-supervisor) id)}
      {:monitor-id id
       :status :supervisor-not-initialized}))) 