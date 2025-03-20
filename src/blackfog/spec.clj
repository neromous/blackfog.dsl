(ns blackfog.spec
  (:require [clojure.edn :as edn]
            [clojure.spec.alpha :as s]
            [clojure.string :as str]))

;; ================ spec ================
;; ================= 基本类型规范 =================
(s/def ::id keyword?)

(s/def ::receiver #{:default
                    :claude
                    :deepseek
                    :extractor
                    :deepseek/origin})

(s/def ::timestamp (s/or :long int?
                         :datetime #(instance? java.time.LocalDateTime %)
                         :instant #(instance? java.time.Instant %)))
(s/def ::status #{:ready :waiting :done :error :processing :completed })
(s/def ::hooks map?)
(s/def ::messages (s/coll-of ::message :kind vector?))
(s/def ::middleware (s/coll-of ifn? :kind vector?)) ; 中间件函数集合
(s/def ::error string?)  ; 添加error字段规范

;; ================= 消息结构规范 =================
(s/def ::role #{"system" "user" "assistant"})
(s/def ::content string?)
(s/def ::think  (s/nilable string?))
(s/def ::parsed map?)  ; 添加parsed字段规范
(s/def ::message
  (s/keys :req-un [::role ::content]
          :opt-un [::think ::timestamp ::parsed ::dimensions ::quantum-state]))

(s/def ::messages (s/coll-of ::message))

;; ================= Meta数据规范 =================
(s/def ::created-at int?)
(s/def ::extractors (s/nilable (s/coll-of any?)))
(s/def ::meta
  (s/keys :req-un [::created-at]
          :opt-un [::extractors]))

;; ================= 接收器规范 =================
(s/def :api/model string?)
(s/def :model/temperature number?)
(s/def :model/top_p number?)
(s/def :model/repetition_penalty number?)
(s/def :model/presence_penalty number?)
(s/def :model/frequency_penalty number?)
(s/def :api/url string?)
(s/def :api/sk string?)

(s/def ::receiver.full
  (s/keys :req [:api/model :api/url :api/sk]
          :opt [::timeout :model/temperature :model/top_p
                :model/repetition_penalty
                :model/presence_penalty
                :model/frequency_penalty]))

;; ================= 请求模板规范 =================
(s/def ::template-element (s/or :keyword keyword?
                                :string string?
                                :symbol symbol?
                                :vector vector?))
(s/def ::template-vector (s/coll-of ::template-element :kind vector?))
(s/def ::template (s/or :fn fn?
                        :vec ::template-vector
                        :nil nil?))

;; ================= 响应规范 =================
(s/def ::response any?)  ; 响应通道可以是任何类型
(s/def ::response-content (s/nilable string?))
(s/def ::response-think (s/nilable string?))
(s/def ::raw-response
  (s/keys :opt-un [::response-content ::response-think]))

;; ================= Validation结果规范 =================
(s/def ::valid boolean?)
(s/def ::reason string?)
(s/def ::validation-result
  (s/keys :req-un [::valid]
          :opt-un [::reason ::buf]))
