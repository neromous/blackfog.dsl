(ns blackfog.dsl.register
  (:require
   [blackfog.dsl.core :refer [reg-element render
                              reg-component]]
   [blackfog.dsl.func.style :as style]
   [blackfog.dsl.func.nexus :as nexus]
   [blackfog.dsl.func.time-misc :as time-misc]
   [clojure.string :as str]
   [blackfog.dsl.func.db :as db]
   [blackfog.dsl.func.file :as file]
   [blackfog.dsl.func.http :as http]
   [blackfog.componments.common.content :as content-comp]
   [blackfog.componments.common.folder :as folder]
   [blackfog.componments.common.file :as file-comp]
   [blackfog.componments.common.proj :as proj]
   [blackfog.dsl.func.folder :as folder-func]
   [blackfog.dsl.func.media :as media]
   [blackfog.componments.preset.eve :as eve]))

;; 消息系统：不推荐使用，推荐使用下面的风格式样
(reg-element :system nexus/system-msg)
(reg-element :user nexus/user-msg)
(reg-element :assistant nexus/assistant-msg)
(reg-element :messages nexus/messages)
;; 消息系统，风格： 对话元素
(reg-element :setting nexus/system-msg)
(reg-element :prompt nexus/user-msg)
(reg-element :reply nexus/assistant-msg)
;; 消息系统，风格： 信息处理
(reg-element :setup nexus/system-msg)
(reg-element :question nexus/user-msg)
(reg-element :answer nexus/assistant-msg)
;; 别名风格： 信息流
(reg-element :config nexus/system-msg)
(reg-element :in nexus/user-msg)
(reg-element :out nexus/assistant-msg)
;; 别名风格： 思维过程
(reg-element :premise nexus/system-msg)
(reg-element :thought nexus/user-msg)
(reg-element :reflection nexus/assistant-msg)

;; 常用交互风格
(reg-element :nexus/ask nexus/question-for)
(reg-element :nexus/asks nexus/questions-for)

;; 原始交互风格
(reg-element :nexus.raw.sync/request nexus/sync-request)
(reg-element :nexus.raw.async/request nexus/async-request)
(reg-element :nexus.raw.async/collect nexus/collect-results)
(reg-element :nexus.raw.extract nexus/response-extractor)

;; 样式系统
(reg-element :p style/p)
(reg-element :h1 style/h1)
(reg-element :h2 style/h2)
(reg-element :h3 style/h3)
(reg-element :h4 style/h4)
(reg-element :bold style/bold)
(reg-element :b style/bold)
(reg-element :row style/row)
(reg-element :rows style/rows)
(reg-element :think style/think)
(reg-element :italic style/italic)
(reg-element :action style/action)
(reg-element :del style/strikethrough)
(reg-element :table style/table)
(reg-element :image style/image)
(reg-element :hr style/hr)
(reg-element :block style/block)
(reg-element :row style/row)
(reg-element :ul style/ul)
(reg-element :ol style/ol)
(reg-element :li style/li)
(reg-element :code style/code)
(reg-element :details style/details)
(reg-element :link style/link)
(reg-element :color style/color)
(reg-element :timeline style/timeline)
(reg-element :alert style/alert)
(reg-element :card style/card)
(reg-element :badge style/badge)
(reg-element :status style/status)
(reg-element :code-inline style/code-inline)

(reg-element :in/text style/text-paragraph)
(reg-element :in/block style/block)
(reg-element :codeblock/json style/format-json)
(reg-element :codeblock/edn style/format-edn)
;; 卡片
(reg-element :todo style/todo)
(reg-element :note style/note)
(reg-element :schedule style/schedule)
(reg-element :calendar style/calendar)

;; 时间函数注册（使用qualified keywords）
(reg-element :time/now time-misc/now)
(reg-element :time/timestamp time-misc/timestamp)
(reg-element :time/yesterday time-misc/yesterday-timestamp)
(reg-element :time/tomorrow time-misc/tomorrow)
(reg-element :time/start-of-today time-misc/start-of-today)
(reg-element :time/end-of-today time-misc/end-of-today)
(reg-element :time/day-of-week time-misc/day-of-week)
(reg-element :time/start-of-week time-misc/start-of-week)
(reg-element :time/end-of-week time-misc/end-of-week)
(reg-element :time/start-of-month time-misc/start-of-month)
(reg-element :time/end-of-month time-misc/end-of-month)
(reg-element :time/start-of-quarter time-misc/start-of-quarter)
(reg-element :time/end-of-quarter time-misc/end-of-quarter)
(reg-element :time/days-between time-misc/days-between)
(reg-element :time/this-week time-misc/this-week)
(reg-element :time/this-month time-misc/this-month)

;; 时间范围快捷方式（可选）
(reg-element :time/today
             (fn [] [(time-misc/start-of-today (time-misc/now))
                     (time-misc/end-of-today (time-misc/now))]))

(reg-element :time/current-week time-misc/this-week)
(reg-element :time/current-month time-misc/this-month)

;; 特殊标签 
(reg-element :style/core-memory style/CoreMemory)
(reg-element :style/status-bar style/status-bar)
(reg-element :style/hidden-secret style/hidden-secret)
(reg-element :style/inner style/inner-thought)

;; 集合操作 
(reg-element :coll/shuffle (fn [& args]
                             (into [] (shuffle args))))
(reg-element :coll/first (fn [& args] (first args)))
(reg-element :coll/take (fn [x args]
                          (str/join "\n" (take x args))))

;; 文件操作
(reg-element :file/info file/file-info)
(reg-element :file/list-dir file/list-dir)
(reg-element :file/read-text file/read-text-file-full)
(reg-element :file/take-lines file/take-lines)
(reg-element :file/take-last-lines file/take-last-lines)
(reg-element :file/take-lines-range file/take-lines-range)
(reg-element :file/read-chunks file/read-text-file-chunks)
(reg-element :file/read-json file/read-json-file)
(reg-element :file/read-edn file/read-edn-file)
(reg-element :file/read-csv file/read-csv-file)
(reg-element :file/search-text file/search-text)

;; 媒体文件操作
(reg-element :media/file-to-base64 media/*file-to-base64)
(reg-element :media/get-image-info media/*get-image-info)
(reg-element :media/read-image-info media/read-image-file-info)
(reg-element :media/read-image-data media/read-image-data)
(reg-element :media/get-audio-info media/*get-audio-info)
(reg-element :media/read-audio-info media/read-audio-file-info)
(reg-element :media/read-audio-data media/read-audio-data)
(reg-element :media/extract-video-frame media/*extract-video-frame)
(reg-element :media/read-video-info media/read-video-file-info)
(reg-element :media/read-video-data media/read-video-data)
(reg-element :media/read-video-frame media/read-video-frame)

;; 添加别名，便于向后兼容
(reg-element :file/read-image-info media/read-image-file-info)
(reg-element :file/read-image-data media/read-image-data)
(reg-element :file/read-audio-info media/read-audio-file-info)
(reg-element :file/read-audio-data media/read-audio-data)
(reg-element :file/read-video-info media/read-video-file-info)
(reg-element :file/read-video-data media/read-video-data)
(reg-element :file/read-video-frame media/read-video-frame)

;; http
(reg-element :http/get http/http-get)
(reg-element :http/post http/http-post)
(reg-element :http/api http/http-api)
(reg-element :http/web http/web-search)

(reg-element :db/create-node db/create-phrase)
(reg-element :db/update-node db/update-phrase)
(reg-element :db/delete-node db/delete-phrase)
(reg-element :db/add-relation db/add-relation)
(reg-element :db/find-relations db/find-relations)
(reg-element :db/create-with-relation db/create-with-relation)
(reg-element :db/domain-knowledge db/domain-knowledge)
(reg-element :db/visualize-domain db/visualize-domain)

;; 文件写入操作
(reg-element :file/write-text file/write-text-file)
(reg-element :file/update-text file/update-text-file)
(reg-element :file/move file/move-file)
(reg-element :file/delete file/delete-file)
(reg-element :file/create-dir file/create-directory)
(reg-element :file/search-files file/search-files)
(reg-element :file/search-content file/search-content)

;; 文件夹操作函数注册
(reg-element :folder/create folder-func/create-folder)
(reg-element :folder/delete folder-func/delete-folder)
(reg-element :folder/move folder-func/move-folder)
(reg-element :folder/copy folder-func/copy-folder)
(reg-element :folder/list folder-func/list-folders)
(reg-element :folder/search folder-func/search-folders)
(reg-element :folder/size folder-func/get-folder-size)
(reg-element :folder/stats folder-func/get-folder-stats)
(reg-element :folder/compare folder-func/compare-folders)
(reg-element :folder/empty folder-func/empty-folder)
(reg-element :folder/exists folder-func/folder-exists?)
(reg-element :folder/tree folder-func/format-folder-tree)

;; 内容组件注册
(reg-component :content/summary content-comp/summary-content-prompt)

;; 文件分析组件注册
(reg-component :file/summary file-comp/file-summary-prompt)
(reg-component :file/extract-entities file-comp/entity-extraction-prompt)
(reg-component :file/analyze-folder file-comp/folder-analysis-prompt)
(reg-component :file/quick-analysis file-comp/file-quick-analysis-prompt)
(reg-component :file/analyze-code file-comp/code-analysis-prompt)
(reg-component :file/extract-keywords file-comp/keyword-extraction-prompt)

;; 文件夹分析组件注册
(reg-component :folder/structure folder/folder-structure-prompt)
(reg-component :folder/recursive-analysis folder/folder-recursive-analysis-prompt)
(reg-component :folder/categorize folder/folder-categorization-prompt)
(reg-component :folder/compare folder/folder-comparison-prompt)
(reg-component :folder/project-structure folder/project-structure-analysis-prompt)
(reg-component :folder/detect-changes folder/folder-change-detection-prompt)
(reg-component :folder/smart-preview folder/folder-smart-preview-prompt)

;; 项目管理组件注册
(reg-component :proj/health-assessment proj/project-health-assessment-prompt)
(reg-component :proj/technical-debt proj/technical-debt-analysis-prompt)
(reg-component :proj/dependency-analysis proj/dependency-analysis-prompt)
(reg-component :proj/code-quality proj/code-quality-analysis-prompt)
(reg-component :proj/documentation-analysis proj/documentation-analysis-prompt)
(reg-component :proj/refactoring-suggestions proj/refactoring-suggestions-prompt)
(reg-component :proj/migration-planning proj/migration-planning-prompt)
(reg-component :proj/roadmap proj/project-roadmap-prompt)




