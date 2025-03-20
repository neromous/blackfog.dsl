(ns blackfog.config.logging
  "日志系统配置，在应用启动时初始化"
  (:require [blackfog.utils.logger :as log]
            [clojure.java.io :as io]
            [clojure.string :as str]))

(defn ensure-log-directory!
  "确保日志目录存在"
  [log-dir]
  (let [dir (io/file log-dir)]
    (when-not (.exists dir)
      (.mkdirs dir))))

(defn get-log-file-path
  "获取日志文件路径，根据环境和日期生成"
  [env]
  (let [date-str (.format (java.time.LocalDate/now)
                          (java.time.format.DateTimeFormatter/ofPattern "yyyy-MM-dd"))
        log-dir "logs"
        filename (format "%s-%s.log" (name env) date-str)]
    (ensure-log-directory! log-dir)
    (str log-dir "/" filename)))

(defn configure-production-logging!
  "配置生产环境日志"
  []
  (log/configure-logging!
   :level :info
   :file (get-log-file-path :prod)
   :format (fn [{:keys [level msg_ timestamp_ ?ns-str ?err]}]
             (str (force timestamp_) " "
                  (str/upper-case (name level)) " "
                  "[" (or ?ns-str "unknown") "] - "
                  (force msg_)
                  (when ?err
                    (str "\n" (-> ?err Throwable->map)))))))

(defn configure-development-logging!
  "配置开发环境日志"
  []
  (log/configure-logging!
   :level :debug))

(defn configure-test-logging!
  "配置测试环境日志"
  []
  (log/configure-logging!
   :level :warn))

(defn init!
  "初始化日志系统，根据环境变量选择配置"
  []
  (let [env (keyword (or (System/getenv "BLACKFOG_ENV") "dev"))]
    (case env
      :prod (configure-production-logging!)
      :test (configure-test-logging!)
      (configure-development-logging!))
    (log/info "日志系统已初始化，环境:" env)))

;; 在 REPL 中使用的便捷函数
(defn set-debug! []
  (log/set-log-level! :debug)
  (log/debug "日志级别已设置为 DEBUG"))

(defn set-info! []
  (log/set-log-level! :info)
  (log/info "日志级别已设置为 INFO"))

(defn set-trace! []
  (log/set-log-level! :trace)
  (log/trace "日志级别已设置为 TRACE"))