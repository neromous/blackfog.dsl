(ns blackfog.utils.logger
  "统一的日志工具，基于 Timbre 实现，提供简单一致的日志接口"
  (:require [taoensso.timbre :as timbre]
            [taoensso.timbre.appenders.core :as appenders]
            [clojure.string :as str]))

;; 默认日志配置
(defn configure-logging!
  "配置日志系统，可选参数包括:
   - :level - 日志级别 (:trace, :debug, :info, :warn, :error, :fatal)
   - :file - 日志文件路径 (如果需要文件输出)
   - :console? - 是否输出到控制台 (默认 true)
   - :format - 日志格式化函数 (默认使用简洁格式)"
  [& {:keys [level file console? format]
      :or {level :info
           console? true
           format (fn [{:keys [level msg_ timestamp_ ?ns-str]}]
                    (str (force timestamp_) " "
                         (str/upper-case (name level)) " "
                         "[" (or ?ns-str "unknown") "] - "
                         (force msg_)))}}]
  
  (let [appenders (cond-> {}
                    console? (assoc :println (-> (appenders/println-appender)
                                                 (assoc :output-fn format)))
                    file (assoc :spit (-> (appenders/spit-appender {:fname file})
                                          (assoc :output-fn format))))]
    
    (timbre/merge-config!
     {:level level
      :appenders appenders})))

;; 初始化默认配置
(configure-logging!)

;; 兼容旧 API 的日志级别设置
(defn set-log-level!
  "设置当前日志级别"
  [level]
  (timbre/set-level! level))

;; 导出 Timbre 的核心日志函数（使用宏包装）
(defmacro trace
  "跟踪级别日志"
  [& args]
  `(timbre/trace ~@args))

(defmacro debug
  "调试级别日志"
  [& args]
  `(timbre/debug ~@args))

(defmacro info
  "信息级别日志"
  [& args]
  `(timbre/info ~@args))

(defmacro warn
  "警告级别日志"
  [& args]
  `(timbre/warn ~@args))

(defmacro error
  "错误级别日志"
  [& args]
  `(timbre/error ~@args))

(defmacro fatal
  "致命错误级别日志"
  [& args]
  `(timbre/fatal ~@args))

;; 添加一些实用的日志辅助函数
(defmacro with-context
  "在指定上下文中执行日志操作"
  [context-map & body]
  `(timbre/with-context+ ~context-map
     (do ~@body nil)))

(defmacro spy
  "记录表达式的值并返回它 (用于调试)"
  [level expr]
  `(timbre/spy ~level ~expr))

;; 使用示例
(comment
  (info "Starting application")
  (debug "Debug information")
  (warn "Warning message")
  (error "Error occurred:" (ex-info "Test error" {}))
  
  ;; 设置日志级别
  (set-log-level! :debug)
  
  ;; 使用上下文
  (with-context {:user-id "123" :request-id "abc"}
    (info "User action"))
  
  ;; 配置日志到文件
  (configure-logging! :level :debug
                     :file "logs/application.log")) 