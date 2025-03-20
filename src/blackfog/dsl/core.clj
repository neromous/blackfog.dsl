(ns blackfog.dsl.core
  (:require [clojure.walk :refer [postwalk]]
            [clojure.core.async :as async]))

;; ================ 定义基础元素 ================
;; 基础元素会经过一次渲染
(def element-registry! (atom {}))

(defn get-elements
  ([] (deref element-registry!))
  ([element-type] (get (deref element-registry!) element-type)))

;; ================ 定义组件 ================
;; 组件会经过二次渲染
(def component-registry! (atom #{}))

(defn get-components
  ([] (deref component-registry!))
  ([component-type] ((deref component-registry!) component-type)))

;; 是否需要等待渲染
(def pending-registry! (atom {}))

(defn set-pending
  ([node] (set-pending (java.util.UUID/randomUUID) node))
  ([id node]
   (get (swap! pending-registry! assoc id node) id)))

;; ================ 基础宏定义 ================
(defn is-form? [form]
  (and (vector? form)
       (not (map-entry? form))
       (let [handler (first form)]
         (or (instance? clojure.lang.Keyword handler)
             (fn? handler)))))

(defn reg-element
  "注册一个基础元素,基础元素是直接渲染的元素"
  [key prompt-fn]
  {:pre [(fn? prompt-fn)
         (keyword? key)]}
  (swap! element-registry! assoc key prompt-fn))

(defn reg-component
  "注册一个组件,组件是一种需要二次渲染的元素"
  [key component-fn]
  {:pre [(fn? component-fn)
         (keyword? key)]}
  (swap! component-registry! conj key)
  (reg-element key component-fn))

;; ================ 定义渲染函数 ================
(defn render
  ;; 渲染节点，使用默认的绑定和最大深度限制
  ([nodes] (render {}  nodes 0 100))

  ;; 渲染节点，使用指定的绑定和默认的最大深度限制
  ([bindings nodes] (render bindings  nodes 0 100))

  ;; 渲染节点，使用指定的绑定、提示注册表、当前深度和最大深度限制
  ([bindings nodes depth max-depth]
   (when (>= depth max-depth)
     (throw (ex-info "Maximum recursion depth exceeded"
                     {:depth depth
                      :max-depth max-depth
                      :nodes nodes})))
   (postwalk
    (fn [node]
      (cond

        ;; 处理函数类型的节点 - 直接执行函数得到结果
        ;; (fn? node) (node)

        ;; hooks 绑定外部变量 - 直接插入绑定值
        (symbol? node) (get bindings node node)

        ;; 处理基础元素
        (is-form? node)
        (let [tag (first node)
              args (rest node)
              metadata (meta node)]
          (cond
            ;; metadata 中有 `:defer` 则不渲染，用于逻辑控制
            (get metadata :defer) node

            ;; metadata 中有 `:pending` 则设置为等待状态,插入标记等待后续渲染
            (get metadata :pending) (str "{{" (set-pending node) "}}")

            ;; 标准处理逻辑
            (keyword? tag)
            (if-let [handler (get-elements tag)]
              (let [result (apply handler args)]
                (cond
                  ;; 如果 注册在component中则将结果作为组件渲染
                  (get-components tag)
                  (render bindings result (inc depth) max-depth)
                  :else result))
              node)

            (fn? tag)
            (let [result (apply tag args)]
              (render bindings result (inc depth) max-depth))

            :else node))

        :else node))
    nodes)))

