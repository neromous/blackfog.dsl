(ns blackfog.dsl.core-test
  (:require [clojure.test :refer [deftest testing is are]]
            [clojure.string :as str]
            [blackfog.dsl.core :refer [reg-element render]]
            [blackfog.dsl.register]))

;; 基础渲染测试
(deftest basic-render-test
  (testing "基本文本渲染"
    (is (string? (render [:p "dddd"])) "应该返回字符串")
    (is (= (render [:p "dddd"]) "dddd") "渲染结果不应为空")
    (is (= (render [:h1 [:p "dddd"]]) "# dddd") "渲染结果不应为空")
    (is (= (render [:h2 [:p "dddd"]]) "## dddd") "渲染结果不应为空")
    (is (= (render [:row [:h1 "foo"]
                    [:p "bar"]]) "# foo\nbar") "h1 渲染错误")
    (is (= (render [:rows [:h1 "foo"]
                    [:p "bar"]]) "# foo\n\nbar") "h2 渲染错误"))
  
  (testing "带绑定的渲染"
    (is (= (render {'?name "张三"} [:p "你好，" '?name]) "你好，张三") "变量绑定渲染错误")
    (is (= (render {'?title "测试标题" '?content "测试内容"} 
                   [:rows [:h1 '?title ]
                    [:p "foo:" '?content]]) 
           "# 测试标题\n\nfoo:测试内容") "嵌套变量绑定渲染错误"))
  
  (testing "样式标签渲染"
    (is (= (render [:bold "重要内容"]) "**重要内容**") "粗体渲染错误")
    (is (= (render [:italic "斜体内容"]) "*斜体内容*") "斜体渲染错误")
    (is (= (render [:code-inline "println(\"Hello\")"]) "`println(\"Hello\")`") "行内代码渲染错误")
    (is (= (render [:alert :warn "警告信息"]) "⚠️ **WARN:** 警告信息") "警告提示渲染错误"))
  
  (testing "列表渲染"
    (is (= (render [:ul "项目1" "项目2"]) " - 项目1\n - 项目2") "无序列表渲染错误")
    (is (= (render [:ol "项目1" "项目2"]) "1. 项目1\n2. 项目2") "有序列表渲染错误"))
  
  (testing "复杂嵌套渲染"
    (is (string? (render [:card "卡片标题" [:p "内容1"] [:p "内容2"]])) "卡片渲染应返回字符串")
    (is (string? (render [:details "摘要" "详细内容"])) "详情渲染应返回字符串")))
;; Nexus对话测试
(deftest nexus-conversation-test
  (testing "基本对话请求"
    (is (string? (render [:nexus/ask :default
                          [:user "你好啊，小猪佩奇"]]))
        "基本对话请求应返回字符串"))

  (testing "使用hook的对话请求"
    (let [result (render {'?setup "你是小猪佩奇"
                          '?user-input "你好啊，小猪佩奇"}
                         [:nexus/ask :default
                          [:system [:p "系统设定:" '?setup]]
                          [:user [:p "我的输入:" '?user-input]]])]
      (is (string? result) "带hook的对话请求应返回字符串")
      (is (pos? (count result)) "结果不应为空")))

  (testing "多轮对话请求"
    (is (string? (render [:nexus/asks :default
                          [[:user "你好啊，小猪佩奇"]]
                          [[:user "你好啊，小羊苏西"]]]))
        "多轮对话请求应返回字符串")))

;; 文本样式测试
(deftest text-style-test
  (testing "基本文本样式"
    (are [style text] (and (string? (render [style text]))
                           (pos? (count (render [style text]))))
      :h1 "这是一级标题"
      :h2 "这是二级标题"
      :p "这是一个段落"
      :bold "这是粗体文本"
      :italic "这是斜体文本"
      :code-inline "println('Hello World')"))

  (testing "列表样式"
    (let [ul-result (render [:ul
                             [:li "无序列表项1"]
                             [:li "无序列表项2"]
                             [:li "无序列表项3"]])
          ol-result (render [:ol
                             [:li "有序列表项1"]
                             [:li "有序列表项2"]
                             [:li "有序列表项3"]])]
      (is (string? ul-result) "无序列表应返回字符串")
      (is (string? ol-result) "有序列表应返回字符串")))

  (testing "复杂样式组合"
    (let [card-result (render [:card
                               [:h3 "卡片标题"]
                               [:p "这是卡片内容"]
                               [:ul
                                [:li "卡片列表项1"]
                                [:li "卡片列表项2"]]])
          details-result (render [:details
                                  [:h3 "详情标题"]
                                  [:p "这是详情内容，点击标题可以展开查看"]])]
      (is (string? card-result) "卡片组件应返回字符串")
      (is (string? details-result) "详情组件应返回字符串"))))

;; 时间操作测试
(deftest time-operations-test
  (testing "当前时间"
    (is (instance? java.time.LocalDateTime (render [:time/now])) "当前时间应返回LocalDateTime对象")
    (is (string? (render [:time/timestamp])) "时间戳应返回字符串"))

  (testing "时间范围"
    (let [today (render [:time/today])
          this-week (render [:time/this-week])
          this-month (render [:time/this-month])]
      (is (vector? today) "今天应返回时间范围向量")
      (is (= 2 (count today)) "今天应返回包含两个元素的向量")
      (is (vector? this-week) "本周应返回时间范围向量")
      (is (vector? this-month) "本月应返回时间范围向量"))))

;; 集合操作测试
(deftest collection-operations-test
  (testing "随机排序"
    (let [original ["A" "B" "C" "D" "E"]
          shuffled (render [:coll/shuffle "A" "B" "C" "D" "E"])]
      (is (vector? shuffled) "应返回向量")
      (is (= (count original) (count shuffled)) "元素数量应保持不变")
      (is (= (set original) (set shuffled)) "元素集合应保持不变")))

  (testing "取第一个元素"
    (is (= "第一" (render [:coll/first "第一" "第二" "第三"]))
        "应返回第一个元素"))

  (testing "取前N个元素"
    (is (= "元素1\n元素2" (render [:coll/take 2 ["元素1" "元素2" "元素3" "元素4"]]))
        "应返回前两个元素，用换行符连接")))

;; 文件操作测试 - 使用实际文件进行测试
(deftest file-operations-test
  (testing "文件操作基本功能检查"
    ;; 这里只检查函数是否已注册，不实际执行文件操作
    (is (fn? (get @blackfog.dsl.core/element-registry! :file/info)) "file/info应已注册")
    (is (fn? (get @blackfog.dsl.core/element-registry! :file/list-dir)) "file/list-dir应已注册")
    (is (fn? (get @blackfog.dsl.core/element-registry! :file/read-text)) "file/read-text应已注册")
    (is (fn? (get @blackfog.dsl.core/element-registry! :file/take-lines)) "file/take-lines应已注册")
    (is (fn? (get @blackfog.dsl.core/element-registry! :file/take-last-lines)) "file/take-last-lines应已注册")
    (is (fn? (get @blackfog.dsl.core/element-registry! :file/take-lines-range)) "file/take-lines-range应已注册")
    (is (fn? (get @blackfog.dsl.core/element-registry! :file/search-text)) "file/search-text应已注册")))

;; 添加详细的文件读取测试
(deftest file-read-operations-test
  (testing "文本文件读取功能"
    ;; 使用playgrounds文件夹中的实际文件
    (let [test-file "playgrounds/test-files/test.txt"]
      (is (string? (render [:file/read-text test-file])) 
          "完整读取文本文件应返回字符串")
      
      (is (.contains (render [:file/read-text test-file]) "这是测试文件内容")
          "文本文件内容应包含预期文本")
      
      (let [lines-result (render [:file/take-lines test-file 3])]
        (is (string? lines-result) "读取前N行应返回字符串")
        (is (.contains lines-result "这是测试文件内容") "前N行应包含第一行内容")
        (is (.contains lines-result "第二行内容") "前N行应包含第二行内容"))
      
      (let [last-lines-result (render [:file/take-last-lines test-file 2])]
        (is (string? last-lines-result) "读取最后N行应返回字符串")
        (is (.contains last-lines-result "第四行内容") "最后N行应包含倒数第二行内容")
        (is (.contains last-lines-result "第五行内容") "最后N行应包含最后一行内容"))
      
      (let [range-lines-result (render [:file/take-lines-range test-file 2 4])]
        (is (string? range-lines-result) "读取指定范围行应返回字符串")
        (is (.contains range-lines-result "第二行内容") "范围行应包含起始行内容")
        (is (.contains range-lines-result "第三行内容") "范围行应包含中间行内容"))))
  
  (testing "结构化文件读取功能"
    ;; 使用playgrounds文件夹中的实际文件
    (let [json-file "playgrounds/test-files/data.json"
          edn-file "playgrounds/test-files/data.edn"
          csv-file "playgrounds/test-files/data.csv"]
      
      (let [json-result (render [:file/read-json json-file])]
        (is (string? json-result) "读取JSON文件应返回字符串")
        (is (.contains json-result ":name") "JSON结果应包含name字段")
        (is (.contains json-result "\"测试\"") "JSON结果应包含测试值"))
      
      (let [edn-result (render [:file/read-edn edn-file])]
        (is (string? edn-result) "读取EDN文件应返回字符串")
        (is (.contains edn-result ":key") "EDN结果应包含key字段")
        (is (.contains edn-result "\"edn-value\"") "EDN结果应包含edn-value值"))
      
      (let [csv-result (render [:file/read-csv csv-file])]
        (is (string? csv-result) "读取CSV文件应返回字符串")
        (is (.contains csv-result "姓名") "CSV结果应包含姓名字段")
        (is (.contains csv-result "张三") "CSV结果应包含张三数据"))))
  
  (testing "文件信息和目录操作"
    ;; 使用playgrounds文件夹中的实际文件和目录
    (let [test-file "playgrounds/test-files/test.txt"
          test-dir "playgrounds/test-files"]
      
      (let [info-result (render [:file/info test-file])]
        (is (string? info-result) "获取文件信息应返回字符串")
        (is (.contains info-result "test.txt") "文件信息应包含文件名")
        (is (.contains info-result "大小:") "文件信息应包含大小信息"))
      
      (let [dir-result (render [:file/list-dir test-dir])]
        (is (string? dir-result) "列出目录内容应返回字符串")
        (is (.contains dir-result "subdir") "目录列表应包含子目录")
        (is (.contains dir-result "test.txt") "目录列表应包含文件")))))

;; 复杂文件操作组合测试
(deftest complex-file-operations-test
  (testing "文件操作与其他功能组合"
    ;; 使用playgrounds文件夹中的实际文件
    (let [test-file "playgrounds/test-files/test.txt"
          json-file "playgrounds/test-files/data.json"]
      
      ;; 测试文件内容与样式组合
      (let [result (render [:card
                            [:h3 "文件内容展示"]
                            [:p [:bold "文件内容:"]]
                            [:code "text" [:file/read-text test-file]]])]
        (is (string? result) "文件内容与样式组合应返回字符串")
        (is (pos? (count result)) "组合渲染结果不应为空")
        (is (.contains result "这是测试文件内容") "渲染结果应包含文件内容"))
      
      ;; 测试JSON数据处理
      (let [json-result (render [:file/read-json json-file])
            result (render [:card
                            [:h3 "JSON数据"]
                            [:p json-result]])]
        (is (string? result) "JSON数据处理应返回字符串")
        (is (pos? (count result)) "处理结果不应为空")
        (is (.contains result "测试") "处理结果应包含JSON数据")))))

;; 综合文件操作与DSL功能测试
(deftest integrated-file-dsl-test
  (testing "文件操作与DSL功能综合使用"
    ;; 使用playgrounds文件夹中的实际文件
    (let [doc-file "playgrounds/test-files/document.md"
          json-file "playgrounds/test-files/data.json"]
      
      ;; 测试场景1：读取文件内容并格式化展示
      (let [result (render [:card
                            [:h2 "文档摘要"]
                            [:p [:bold "来源: "] doc-file]
                            [:p [:italic "内容预览:"]]
                            [:file/read-text doc-file]])]
        (is (string? result) "文档摘要应返回字符串")
        (is (pos? (count result)) "文档摘要不应为空")
        (is (.contains result "标题") "文档摘要应包含文件内容"))
      
      ;; 测试场景2：读取JSON数据并生成报告
      (let [json-result (render [:file/read-json json-file])
            report (render [:rows
                            [:h1 "用户报告"]
                            [:p "JSON数据: " json-result]])]
        (is (string? report) "用户报告应返回字符串")
        (is (pos? (count report)) "用户报告不应为空")
        (is (.contains report "用户报告") "报告应包含标题")
        (is (.contains report "张三") "报告应包含用户数据"))
      
      ;; 测试场景3：使用模板和数据生成文档
      (let [doc-content (render [:file/read-text doc-file])
            template {'?title "自动生成的报告"
                      '?date (render [:time/now])
                      '?author "系统"
                      '?content doc-content}
            document (render template
                             [:rows
                              [:h1 '?title]
                              [:p [:bold "日期: "] '?date]
                              [:p [:bold "作者: "] '?author]
                              [:hr]
                              [:p '?content]
                              [:hr]
                              [:p [:italic "自动生成，请勿回复"]]])]
        (is (string? document) "生成的文档应为字符串")
        (is (pos? (count document)) "生成的文档不应为空")
        (is (.contains document "标题") "生成的文档应包含文件内容")))))

;; HTTP操作测试 - 使用mock进行测试
(deftest http-operations-test
  (testing "HTTP操作基本功能检查"
    ;; 这里只检查函数是否已注册，不实际执行网络请求
    (is (fn? (get @blackfog.dsl.core/element-registry! :http/get)) "http/get应已注册")
    (is (fn? (get @blackfog.dsl.core/element-registry! :http/post)) "http/post应已注册")
    (is (fn? (get @blackfog.dsl.core/element-registry! :http/api)) "http/api应已注册")
    (is (fn? (get @blackfog.dsl.core/element-registry! :http/web)) "http/web应已注册")))

;; 数据库操作测试 - 使用mock进行测试
(deftest db-operations-test
  (testing "数据库操作基本功能检查"
    ;; 这里只检查函数是否已注册，不实际执行数据库操作
    (is (fn? (get @blackfog.dsl.core/element-registry! :db/create-node)) "db/create-node应已注册")
    (is (fn? (get @blackfog.dsl.core/element-registry! :db/update-node)) "db/update-node应已注册")
    (is (fn? (get @blackfog.dsl.core/element-registry! :db/delete-node)) "db/delete-node应已注册")
    (is (fn? (get @blackfog.dsl.core/element-registry! :db/add-relation)) "db/add-relation应已注册")
    (is (fn? (get @blackfog.dsl.core/element-registry! :db/find-relations)) "db/find-relations应已注册")
    (is (fn? (get @blackfog.dsl.core/element-registry! :db/create-with-relation)) "db/create-with-relation应已注册")
    (is (fn? (get @blackfog.dsl.core/element-registry! :db/visualize-domain)) "db/visualize-domain应已注册")))

;; 复杂组合测试
(deftest complex-composition-test
  (testing "样式和时间组合"
    (let [result (render
                  [:card
                   [:h3 "今日任务清单"]
                   [:p [:bold "日期: "] [:time/now]]
                   [:ul
                    [:li [:todo :medium "完成DSL测试用例" false]]
                    [:li [:todo :medium "实现文件操作功能" false]]
                    [:li [:todo :medium "优化渲染性能" false]]]])]
      (is (string? result) "组合渲染应返回字符串")
      (is (pos? (count result)) "组合渲染结果不应为空")))

  (testing "使用hook和样式组合"
    (let [result (render {'?title "项目进度报告"
                          '?date (render [:time/now])
                          '?progress "当前进度待完善"}
                         [:card
                          [:h2 '?title]
                          [:p [:bold "报告日期: "] '?date]
                          [:p "当前进度: " '?progress "%"]
                          [:status :info '?progress]
                          [:ul
                           [:li "已完成: 基础架构设计"]
                           [:li "已完成: 核心功能实现"]
                           [:li "进行中: 测试用例编写"]
                           [:li "待完成: 文档撰写"]]])]
      (is (string? result) "带hook的组合渲染应返回字符串")
      (is (pos? (count result)) "带hook的组合渲染结果不应为空")))

  (testing "嵌套DSL示例"
    (let [result (render
                  [:nexus/ask :default
                   [:system
                    [:h1 "你是一位专业的数据分析师"]
                    [:p "请根据以下数据提供分析:"]]
                   [:user
                    [:card
                     [:h3 "销售数据"]
                     [:table
                      ["月份" "销售额" "增长率"]
                      ["1月" "10000" "5%"]
                      ["2月" "12000" "20%"]
                      ["3月" "11500" "-4%"]
                      ["4月" "13500" "17%"]]]]])]
      (is (string? result) "嵌套DSL渲染应返回字符串")
      (is (pos? (count result)) "嵌套DSL渲染结果不应为空"))))

;; 多模态组合测试 - 仅检查函数注册
(deftest multimodal-composition-test
  (testing "多模态组件注册检查"
    (is (fn? (get @blackfog.dsl.core/element-registry! :image)) "image组件应已注册")))


