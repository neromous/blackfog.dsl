(ns user
  (:require [blackfog.utils.logger :as log]
            [clojure.tools.namespace.repl :refer [refresh refresh-all]]
            [blackfog.dsl.core :as dsl]
            [blackfog.dsl.register]
            [clojure.pprint :as pp]))
;; app-state

(defonce app-state (atom {:linxia/speak []}))

(defn show-state []
  (clojure.pprint/pprint @app-state))

;; 配置开发环境的日志输出
(log/configure-logging! :level :info)

(defn reset
  "重置系统状态，用于开发时重新加载代码"
  []
  (refresh))

(defn reset-all
  "完全重置系统状态，包括所有namespace"
  []
  (refresh-all))

;; REPL 辅助函数
(defn reload-prompt
  "重新加载并测试 prompt"
  [prompt-form]
  (try
    (let [result (dsl/render prompt-form)]
      (log/info "Prompt 测试成功")
      result)
    (catch Exception e
      (log/error "Prompt 测试失败:" (.getMessage e))
      (throw e))))


