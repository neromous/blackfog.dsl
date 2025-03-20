# BlackFog DSL 开发手册

## 1. 系统概述

BlackFog DSL 是一个灵活的领域特定语言系统，设计用于简化与大语言模型的交互、知识管理和各种辅助功能的实现。系统采用声明式语法，允许通过简单的向量表示来构建复杂的提示和功能调用。

### 1.1 核心理念

- **声明式语法**：使用向量表示的DSL，简洁直观
- **组件化设计**：功能被封装为可重用的组件
- **可扩展性**：易于添加新组件和功能
- **一致性**：统一的接口和命名约定
- **响应式设计**：采用类似Reagent的函数式组件模型

### 1.2 系统架构

BlackFog DSL 系统由以下主要部分组成：

1. **DSL 核心**：提供基础的DSL解析和渲染功能
2. **组件注册系统**：管理和注册可用的DSL组件
3. **函数式组件系统**：支持复杂组件的组合和嵌套渲染
4. **渲染引擎**：将DSL表达式转换为实际运行的代码或文本
5. **功能模块**：提供文件操作、数据处理、网络请求等功能
   - **文件系统**：提供文件和文件夹的读写、检索和管理功能
   - **媒体处理**：支持图像、音频和视频文件的处理和分析
   - **网络通信**：支持HTTP请求和API调用
   - **数据库操作**：提供图数据库的交互功能
   - **时间处理**：提供时间相关的函数和工具
   - **消息系统**：支持与各种LLM模型的交互
   - **控制流**：提供条件判断和循环等控制结构
   - **样式系统**：提供文本格式化和样式设置功能

## 2. 基本语法

BlackFog DSL 使用Clojure风格的向量语法，但经过简化和专门化，使其更适合由文本智能体使用。

### 2.1 基本结构

DSL表达式的基本结构如下：

```clojure
[:tag param1 param2 ...]
```

其中:
- `:tag` 是一个关键字，指定要使用的功能或组件
- `param1`, `param2` 等是传递给该功能或组件的参数

### 2.2 嵌套结构

DSL表达式可以嵌套：

```clojure
[:outer-tag
  [:inner-tag1 param1]
  [:inner-tag2 param2 param3]]
```

### 2.3 渲染过程

DSL表达式渲染分两种情况：
- 基础元素：直接渲染为结果
- 组件：先渲染为新的DSL表达式，然后再渲染结果

## 3. 功能模块详解

### 3.1 消息系统

BlackFog DSL 提供了多种风格的消息系统组件，用于与大语言模型进行交互：

```clojure
;; 标准消息格式
[:system "系统提示内容"]
[:user "用户问题"]
[:assistant "助手回复"]
[:messages [...消息数组...]]

;; 风格化消息格式
[:setting "系统提示内容"]  ;; 对话元素风格
[:prompt "用户问题"]
[:reply "助手回复"]

;; 信息处理风格
[:setup "系统提示内容"]
[:question "用户问题"]
[:answer "助手回复"]

;; 信息流风格
[:config "系统提示内容"]
[:in "用户输入"]
[:out "系统输出"]

;; 思维过程风格
[:premise "系统思考前提"]
[:thought "思考过程"]
[:reflection "反思结果"]
```

### 3.2 样式系统
为了适应大语言模型对token的敏感性，因此扩展出声明式，样式系统，保证格式稳定。

样式系统提供丰富的文本格式化和标记功能：

```clojure
;; 基本文本样式
[:h1 "一级标题"]
[:h2 "二级标题"]
[:p "段落文本"]
[:bold "粗体文本"]
[:italic "斜体文本"]
[:code "代码块" "语言"]
[:code-inline "行内代码"]

;; 列表和表格
[:ul [:li "无序列表项1"] [:li "无序列表项2"]]
[:ol [:li "有序列表项1"] [:li "有序列表项2"]]
[:table [...表格数据...]]

;; 特殊元素
[:image "图片URL" "可选标题"]
[:link "链接文本" "URL"]
[:hr] ;; 水平分割线
[:block "块级引用内容"]
```

### 3.3 文件系统操作

文件系统模块提供完整的文件和文件夹操作功能：

```clojure
;; 文件读取
[:file/info "path/to/file.txt"]
[:file/read-text "path/to/file.txt"]
[:file/take-lines "path/to/file.txt" 10]
[:file/take-last-lines "path/to/file.txt" 10]
[:file/take-lines-range "path/to/file.txt" 5 15]
[:file/read-json "path/to/data.json"]
[:file/read-edn "path/to/data.edn"]
[:file/read-csv "path/to/data.csv"]

;; 文件写入
[:file/write-text "path/to/file.txt" "内容"]
[:file/update-text "path/to/file.txt" "新内容"]
[:file/move "source/path.txt" "dest/path.txt"]
[:file/delete "path/to/file.txt"]

;; 搜索和查询
[:file/search-files "path" "pattern"]
[:file/search-content "path" "内容关键词"]

;; 文件夹操作
[:folder/create "path/to/folder"]
[:folder/list "path/to/parent"]
[:folder/tree "path/to/parent" max-depth]
[:folder/stats "path/to/folder"]
```

### 3.4 媒体处理

媒体处理模块支持图像、音频和视频文件的处理：

```clojure
;; 图像处理
[:media/read-image-info "path/to/image.jpg"]
[:media/read-image-data "path/to/image.jpg"]

;; 音频处理
[:media/read-audio-info "path/to/audio.mp3"]
[:media/read-audio-data "path/to/audio.mp3"]

;; 视频处理
[:media/read-video-info "path/to/video.mp4"]
[:media/read-video-data "path/to/video.mp4"]
[:media/read-video-frame "path/to/video.mp4" time-in-seconds]
```

### 3.5 网络通信

网络模块提供HTTP请求和Web搜索功能：

```clojure
;; HTTP请求
[:http/get "https://api.example.com/data"]
[:http/post "https://api.example.com/submit" {:param1 "value1"}]

;; API调用和搜索
[:http/api :weather "forecast" {:city "Beijing"}]
[:http/web "搜索关键词"]
```

### 3.6 数据库操作

图数据库模块提供知识管理和关系处理功能：

```clojure
;; 节点操作
[:db/create-node {:phrase/title "概念名称" :phrase/content "概念内容"}]
[:db/update-node "概念名称" {:phrase/content "新内容"}]
[:db/delete-node "概念名称"]

;; 关系操作
[:db/add-relation "概念A" "概念B" :is-a]
[:db/find-relations "概念名称"]

;; 领域知识
[:db/domain-knowledge "领域名称"]
[:db/visualize-domain "领域名称"]
```

### 3.7 时间处理

时间模块提供各种时间相关的工具函数：

```clojure
;; 基本时间
[:time/now]
[:time/timestamp]
[:time/yesterday]
[:time/tomorrow]

;; 时间范围
[:time/today]
[:time/this-week]
[:time/this-month]
[:time/start-of-week [:time/now]]
[:time/end-of-month [:time/now]]

;; 时间计算
[:time/days-between start-time end-time]
```

### 3.8 组件系统

BlackFog DSL 提供丰富的预设组件，用于常见任务：

```clojure
;; 文件分析组件
[:file/summary "path/to/file.txt"]
[:file/extract-entities "path/to/file.txt"]
[:file/analyze-code "path/to/code.clj"]

;; 文件夹分析组件
[:folder/structure "path/to/folder"]
[:folder/recursive-analysis "path/to/folder"]
[:folder/smart-preview "path/to/folder"]

;; 项目管理组件
[:proj/health-assessment "path/to/project"]
[:proj/technical-debt "path/to/project"]
[:proj/dependency-analysis "path/to/project"]
```

## 4. 高级功能

### 4.1 请求模式

BlackFog DSL 支持多种请求模式：

```clojure
;; 同步请求
[:nexus.raw.sync/request 
  {:receiver :default}
  [:messages ...]]

;; 异步请求
[:nexus.raw.async/request 
  {:receiver :GPT4}
  [:messages ...]]

;; 结果收集
[:nexus.raw.async/collect chan1 chan2 ...]

;; 快捷方式
[:nexus/ask :default 
  [:user "问题内容"]
  [:system "系统提示"]]
```

### 4.2 内容提取

系统支持从响应中提取特定内容：

```clojure
[:nexus.raw.extract response]  ;; 提取内容和思考部分
```

### 4.3 集合操作

提供常见的集合处理函数：

```clojure
[:coll/shuffle item1 item2 item3]
[:coll/first collection]
[:coll/take n collection]
```

## 5. 扩展与定制

BlackFog DSL 系统设计为可扩展的，开发者可以通过以下方式添加新功能：

### 5.1 注册新元素

```clojure
(reg-element :my-tag my-handler-fn)
```

### 5.2 注册新组件

```clojure
(reg-component :my-component my-component-fn)
```

### 5.3 使用元数据控制渲染

```clojure
;; 延迟渲染
^{:defer true} [:my-tag ...]

;; 待决渲染
^{:pending true} [:my-tag ...]
```

## 6. 实际应用示例

### 6.1 基本提示构建

```clojure
[:messages
 [:system "你是一个专业的数据分析师"]
 [:user "请分析以下数据趋势:\n" [:file/read-text "data.csv"]]
 [:assistant "根据数据分析..."]]
```

### 6.2 带样式的内容生成

```clojure
[:messages
 [:system 
  [:h1 "专业写作助手"]
  [:p "你是一位精通写作的AI助手，擅长创建高质量的内容。"]
  [:ul
   [:li "使用清晰简洁的语言"]
   [:li "保持专业的语调"]
   [:li "提供有价值的见解"]]]
 [:user "请为我的技术博客写一篇关于机器学习的介绍"]]
```

### 6.3 文件分析工作流

```clojure
[:nexus/ask :default
 [:system "你是一位代码分析专家"]
 [:user "请分析这个文件的功能和结构:\n" 
  [:file/read-text "src/core.clj"]]]
```

### 6.4 多次交互的工作流

```clojure
;; 第一次请求
(def result1 
  [:nexus.raw.sync/request 
   {:receiver :default} 
   [:messages
    [:system "你是一位数据分析专家"]
    [:user "请分析这个CSV文件:\n" [:file/read-text "data.csv"]]]])

;; 提取第一次响应
(def extracted1 [:nexus.raw.extract result1])

;; 基于第一次响应进行第二次请求
[:nexus.raw.sync/request 
 {:receiver :default}
 [:messages
  [:system "你是一位数据可视化专家"]
  [:user "基于之前的分析:\n" (:content extracted1) "\n请推荐合适的可视化方式"]]]
```

## 7. 最佳实践

1. **组件化设计**：将常用功能封装为组件，提高代码重用性
2. **命名空间划分**：使用命名空间前缀区分不同类别的功能
3. **参数验证**：在函数内部对参数进行验证，提高健壮性
4. **错误处理**：使用try-catch包装可能出错的操作，提供友好的错误信息
5. **文档完善**：为每个函数和组件提供详细的文档注释
6. **逐步渲染**：对于复杂操作，考虑使用多步渲染以提高性能
7. **缓存结果**：对于昂贵的操作，考虑缓存结果以提高响应速度