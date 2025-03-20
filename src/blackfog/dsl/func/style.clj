(ns blackfog.dsl.func.style
  (:require [clojure.string :as str]
            [clojure.pprint :as pp]
            [clojure.spec.alpha :as s]
            [blackfog.spec :as spec]
            [cheshire.core :as json])
  (:import [java.time LocalDateTime LocalDate ZonedDateTime ZoneId]
           [java.time.format DateTimeFormatter]
           [java.time.temporal ChronoUnit]))

;; markdown styles
(defn p [& args]
  (str/join   (map (comp str/trim str) args)))

(defn row [& args]
  (str/join "\n"  (map (comp str/trim str) args)))

(defn rows [& args]
  (str/join "\n\n"  (map (comp str/trim str) args)))

(defn bold [& args]
  (str "**" (str/trim (str/join args)) "**"))

(defn action [& args]
  (str "<action>" (str/trim (str/join args)) "</action>"))

(defn CoreMemory [& args]
  (str "<CoreMemory>" (str/trim (str/join args)) "</CoreMemory>"))

(def b bold)

(defn think [& args]
  (str "<think>\n" (apply p args) "\n</think>"))

(defn italic [& args]
  (str "*" (apply p args) "*"))

(defn strikethrough [& args]
  (str "~~" (apply p args) "~~"))

;; markdown
(defn h1 [& args] (str "# " (apply p args)))

(defn h2 [& args] (str "## " (apply p args)))

(defn h3 [& args] (str "### " (apply p args)))

(defn h4 [& args] (str "#### " (apply p args)))

(defn title [& args] (str "* " (apply p args)))

;; table
(defn table [headers & rows]
  (let [divider (str/join "|" (repeat (count headers) "---"))
        header-row (str/join "|" headers)]
    (str/join "\n"
              (concat [header-row (str "|" divider "|")]
                      (map #(str/join "|" %) rows)))))

(defn image [url alt-text & [title]]
  (let [title-part (when title (str " \"" title "\""))]
    (str "![" alt-text "]" "(" url title-part ")")))

(defn hr [] "\n---\n")

(defn ul [& items]
  (let [prefixes (repeat " - ")]
    (str/join "\n" (map #(str %1 (if (map? %2)
                                   (pr-str %2)
                                   %2))
                        prefixes items))))

(defn ol [& items]
  (let [prefixes (map #(str (inc %1) ". ") (range (count items)))]
    (str/join "\n" (map #(str %1 (if (map? %2)
                                   (pr-str %2)
                                   %2))
                        prefixes items))))

(defn li [& args]
  (str (apply p args)))

(defn li-task [checked? & args]
  (str "[" (if checked? "x" " ") "] " (apply p args)))

;;  code block
(defn GATE [role status-bar]
  (str "<|GATE|> " "**" role "**: " status-bar))

(defn inner-thought [& args]
  (str "\n> 💭 " (str/join (map (comp str/trim str) args)) "\n"))

;; 代码块
(defn code-inline [& args]
  (str "`" (str/join " " (map (comp str/trim str) args))  "`"))

(defn code [code args]
  (str "\n```" code "\n" args "\n```\n"))

(defn block [args]
  (str "\n```\n" args "\n```\n"))

(defn text-paragraph [args]
  (str "<<<" args ">>>"))

(defn format-edn [obj]
  (str "\n```edn\n\n"
       (with-out-str
         (clojure.pprint/pprint obj))
       "\n```\n"))

(defn format-json [obj]
  (str "\n```json\n"
       (json/generate-string obj {:pretty true})
       "\n```\n"))

(defn status [type & args]
  (let [icon (case type
               :success "✅"
               :warning "⚠️"
               :error   "❌"
               :info    "ℹ️"
               "")]
    (str icon " " (str/join "\n" (map (comp str/trim str) args)))))

(defn card [title & content]
  (str "### " title "\n"
       "---\n"
       (str/join "\n" (map (comp str/trim str) content))
       "\n---\n"))

(defn badge [label value & [color]] ; 修正参数名拼写错误
  (let [color (or color "blue")]
    (str "![" label "](https://img.shields.io/badge/"
         (str/replace label " " "_") "-"
         (str/replace value " " "_") "-"
         color ")")))

(defn alert [type & content]
  (let [icon (case type
               :tip    "💡"
               :note   "📝"
               :warn   "⚠️"
               :danger "🚨"
               :bug    "🐛"
               :rocket "🚀"
               "ℹ️")]
    (str icon " **" (str/upper-case (name type)) ":** "
         (str/join " " content))))

(defn details [summary & content]
  (str "<details>\n<summary>" summary "</summary>\n\n"
       (apply p content)
       "\n</details>"))

(defn link [& links]
  (when (odd? (count links))
    (throw (Exception. "link function requires even number of arguments")))
  (str/join "\n" (for [[text url] (partition 2 links)]
                   (str "- [" text "](" url ")"))))

(defn color [hex & args]
  (str "<span style=\"color:" hex "\">" (apply p args) "</span>"))

(defn timeline [& events]
  (str/join "\n" (for [[time desc] (partition 2 events)]
                   (str "- **" time "**: " desc))))

(defn todo
  "生成待办事项项
  (todo :high '完成设计稿' true) => - [x] ⚡完成设计稿"
  [priority task & [checked?]]
  (let [priority-icon (case priority
                        :high "⚡"
                        :medium "🔥"
                        :low "🌱"
                        "•")
        checked (if checked? "x" " ")]
    (str "- [" checked "] " priority-icon " " task)))

(defn note
  "生成笔记卡片
  (note '会议纪要' '讨论项目里程碑')"
  [title & content]
  (str "### 📝 " title "\n"
       "---\n"
       (str/join "\n" (map (comp str/trim str) content))
       "\n---\n"))

(defn schedule
  "生成日程安排项
  (schedule '2023-10-10 14:00' '项目评审会')"
  [time & events]
  (str "**⏰ " time "**\n"
       (str/join "\n" (map #(str "   ▸ " %) events))))

(defn calendar
  "生成日程表视图
  (calendar '十月' ['1日 立项会' '5日 原型评审' '20日 测试']"
  [month & items]
  (str "📅 **" month "日程**\n"
       (str/join "\n"
                 (map #(str " ▸ " %) items))))

;; 新增人物设定相关标签
(defn persona-setup
  "处理人物设定，支持结构化的人设数据
   参数可以是map或键值对列表
   示例: [:persona/setup {:name \"角色名\" :type \"类型\"}]
         [:persona/setup :name \"角色名\" :type \"类型\"]"
  [& args]
  (let [persona-data (if (and (= 1 (count args)) (map? (first args)))
                       (first args)  ;; 如果是单个map参数
                       (apply hash-map args))  ;; 如果是键值对列表

        ;; 提取常见人设属性，支持多种可能的键名
        name (or (:name persona-data)
                 (:identifier persona-data)
                 (:figure/name persona-data)
                 (get persona-data :name))

        type (or (:type persona-data)
                 (:figure/type persona-data)
                 "未指定")

        status (or (:status persona-data)
                   (:figure/status persona-data)
                   (:current-status persona-data)
                   "未指定")

        background (or (:background persona-data)
                       (:figure/setup persona-data)
                       (:backstory persona-data)
                       "未指定")

        appearance (or (:appearance persona-data)
                       (:figure/appearance persona-data)
                       (:look persona-data)
                       "未指定")

        primary-language (or (:primary-language persona-data)
                             (:main-language persona-data)
                             "未指定")

        secondary-languages (or (:secondary-languages persona-data)
                                (:other-languages persona-data)
                                [])

        traits (or (:traits persona-data)
                   (:characteristics persona-data)
                   (:特质 persona-data)
                   [])

        ;; 处理其他自定义属性
        custom-attrs (dissoc persona-data
                             :name :identifier :figure/name
                             :type :figure/type
                             :status :figure/status :current-status
                             :background :figure/setup :backstory
                             :appearance :figure/appearance :look
                             :primary-language :main-language
                             :secondary-languages :other-languages
                             :traits :characteristics :特质)

        ;; 格式化函数
        format-value (fn [v]
                       (cond
                         (vector? v) (str/join ", " v)
                         (map? v) (str/join "\n" (map (fn [[k v]] (str "  - " (name k) ": " v)) v))
                         :else (str v)))

        ;; 构建基本人设信息
        basic-info [(str "## " (if (string? name) name (pr-str name)) " 人物设定")
                    (str "1. 类型：" (format-value type))
                    (str "2. 当前状态：" (format-value status))
                    (str "3. 背景：" (format-value background))
                    (str "4. 外貌：" (format-value appearance))
                    (str "5. 主要语言：" (format-value primary-language))]

        ;; 添加次要语言（如果有）
        with-secondary (if (seq secondary-languages)
                         (conj basic-info (str "6. 次要语言：" (format-value secondary-languages)))
                         basic-info)

        ;; 添加特质（如果有）
        with-traits (if (seq traits)
                      (conj with-secondary
                            (str "\n## 特质/特性\n"
                                 (str/join "\n"
                                           (map-indexed
                                            (fn [idx trait]
                                              (str (inc idx) ". " trait))
                                            (if (coll? traits) traits [traits])))))
                      with-secondary)

        ;; 添加自定义属性（如果有）
        with-custom (if (seq custom-attrs)
                      (conj with-traits
                            (str "\n## 其他属性\n"
                                 (str/join "\n"
                                           (map (fn [[k v]]
                                                  (str "- " (name k) ": " (format-value v)))
                                                custom-attrs))))
                      with-traits)]

    ;; 连接所有部分并返回
    (str/join "\n\n" (map str/trim with-custom))))

(defn status-bar [& sections]
  (str/join "\n\n---\n"
            (map (fn [section]
                   (str "### " (first section) "\n"
                        (str/join "\n" (rest section))))
                 sections)))

;; 新增hidden-secret标签
(defn hidden-secret [& content]
  (str "🔒 [机密档案]\n"
       (str/join "\n" (map #(str "• " %) content))
       "\n🔒 [档案结束]"))


