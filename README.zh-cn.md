# BlackFog

BlackFog is a powerful framework providing a domain-specific language (DSL) for complex knowledge processing and LLM integration.

## 项目概述

BlackFog 是一个灵活、组合式的知识处理框架，它提供了一套声明式 DSL (领域特定语言)，用于表达和处理复杂的知识结构。BlackFog 通过向量化语法和组件化设计，简化了知识抽取、转换和交互的过程，特别适合于与大型语言模型 (LLM) 集成的应用场景。

核心设计理念：
- **声明式**：使用简洁明了的向量语法描述复杂操作
- **组合式**：支持函数式组合，轻松构建复杂工作流
- **可扩展**：模块化设计，易于扩展新功能和集成外部服务

## Agent 框架架构

BlackFog 的核心是一个强大的 Agent 框架，它通过嵌套模板和组合方式构建复杂的智能体系统。这种方法使您能够构建复杂、层次化的 Agent 系统，每个 Agent 都具有专门的能力：

### 复合 Agent 模式

BlackFog 的组合式设计允许通过嵌套模板创建复合 Agent：

- **基础 Agent**：定义作为简单元素的核心功能
- **复合 Agent**：将多个基础 Agent 组合成更复杂的结构
- **Agent 专业化**：创建针对特定领域或任务的专业化 Agent

```clojure
;; 示例：通过组合创建专业代码分析 Agent
[:nexus/ask 
 {:receiver :Gemini}  ;; Agent 选择
 [:system             ;; 系统提示定义 Agent 个性/能力
  [:h1 "你是一位专业的代码分析 Agent"]
  [:p "你的专长包括识别架构模式和提出改进建议。"]]
 [:user               ;; 嵌套分析组件
  [:file/analyze-code "src/core/architecture.clj"]
  [:proj/code-quality "src/core"]]]
```

### 多级渲染

框架通过多级渲染处理嵌套的 Agent 定义：

1. **元素渲染**：基础元素只渲染一次
2. **组件渲染**：组件会渲染两次，首先生成新表达式，然后计算该表达式
3. **递归深度控制**：控制嵌套深度以防止无限递归

这种多级渲染方法实现了：
- Agent 行为的渐进式细化
- 基于上下文的动态适应
- 跨组件边界的结构化推理

### Agent 通信

Agent 可以通过以下方式进行通信和共享上下文：

- **共享绑定**：在组件之间传递变量和上下文
- **消息传递**：Agent 组件之间的结构化消息交换
- **响应式处理**：Agent 组件之间流式数据传输，实现实时处理

```clojure
;; 示例：Agent 通过共享上下文协作
(def bindings {'analysis-results (render [:proj/code-quality "src/core"])
               'project-context {:name "BlackFog" :type "Framework"}})

(render bindings 
       [:nexus/ask 
        {:receiver :default}
        [:system "你是一位重构专家。"]
        [:user "基于以下分析，提出改进建议："
         [:p "分析结果: " 'analysis-results]
         [:p "项目上下文: " 'project-context]]])
```

### 专业化 Agent 类型

BlackFog 通过其组件系统支持各种 Agent 类型：

- **知识处理 Agent**：提取、转换和推理信息
- **任务专业化 Agent**：专注于特定操作，如文件分析或代码重构
- **协调者 Agent**：协调多个 Agent 解决复杂问题
- **多模态 Agent**：处理和生成不同格式（文本、代码、图像）的内容

### Agent 框架核心优势

- **声明式定义**：使用直观的向量语法定义复杂的 Agent 行为
- **渐进式增强**：从简单开始，逐步添加能力
- **领域特定专业化**：创建针对特定领域或任务定制的 Agent
- **可重用组件**：利用预构建组件加速 Agent 开发
- **测试和调试**：在不同组合层级测试 Agent

### 项目目标

BlackFog 旨在实现真正的响应式、声明式编程范式，帮助开发者构建更加健壮、灵活的知识处理系统：

1. **声明式编程的实践**：通过向量化DSL，让开发者专注于描述"做什么"而非"怎么做"，减少认知负担
2. **响应式数据流**：利用 core.async 提供响应式编程支持，实现数据的异步处理和流式传输
3. **函数式不变性**：遵循函数式编程理念，减少副作用，提高代码的可预测性和可测试性
4. **大模型能力增强**：通过结构化知识表示和处理，增强大型语言模型的推理和生成能力

这些目标使 BlackFog 成为一个理想的工具，特别适用于：
- 知识密集型应用开发
- 复杂数据流处理系统
- LLM 赋能的智能应用
- 需要灵活扩展的企业应用集成

## 系统架构

BlackFog 系统由以下核心部分组成：

1. **DSL 核心** - 提供灵活的领域特定语言，用于表达和处理复杂的知识结构
   - 基础元素系统（Element Registry）
   - 组件系统（Component Registry）
   - 渲染引擎（Render Engine）
2. **LLM 集成** - 与大型语言模型的集成，提供智能分析和生成能力
   - 多模型支持（OpenAI, Gemini等）
   - 流式响应处理
   - 对话历史管理
3. **功能模块** - 提供丰富的功能模块，包括：
   - 文件和文件夹操作
   - 知识图谱管理
   - HTTP 请求处理
   - 媒体文件处理

## 核心功能

### DSL 系统

BlackFog 的 DSL 系统允许以声明式方式表达复杂操作，主要特点包括：

- 基于向量的语法，易于阅读和编写
- 支持组件化和函数式组合
- 灵活的渲染和转换机制

```clojure
;; DSL 示例 - HTTP 请求
[:http/get "https://example.com/api/data"]

;; DSL 示例 - 用户问答
[:nexus/ask
 {:receiver :default}
 "请解释量子计算的基本原理"]
```

### 文件和文件夹管理

系统提供全面的文件和文件夹操作功能：

- 文件读写、移动、删除
- 文件夹结构分析和可视化
- 智能文件内容分析和摘要

```clojure
;; 文件读取示例
[:file/read-text "path/to/file.txt"]

;; 文件夹结构分析
[:folder/structure "path/to/project"]
```

### 知识图谱

BlackFog 包含一个图数据库系统，用于构建和查询知识图谱：

- 创建和管理知识节点（短语）
- 建立节点间的关系
- 按领域组织和可视化知识

```clojure
;; 创建知识节点
[:db/create-node {:phrase/title "量子计算"
                  :phrase/content "量子计算是利用量子力学现象进行信息处理的计算方式"
                  :phrase/domain ["物理学" "计算机科学"]
                  :phrase.related/words ["量子位" "量子纠缠"]}]
```


## 使用示例

### 基本 DSL 用法

```clojure
;; 发送 HTTP 请求
[:http/get "https://baike.baidu.com/item/量子计算"]

;; 简单的用户问答
[:nexus/ask
 {:receiver :default}
 "请解释量子计算的基本原理"]

;; 带有系统提示的对话
[:nexus/ask
 {:receiver :Gemini}
 {:system [:h1 "你是一位量子物理学专家，请用通俗易懂的语言回答问题"]
           [:ul :ordered 
            [:li "你拥有clojure编程经验"]
            [:li "你的代码能力出众，遵守'KISS'原则"]]]
  :prompt "什么是量子纠缠？"}]
```

### 复合 Agent 示例

BlackFog 支持通过嵌套模板创建复杂的复合 Agent：

```clojure
;; 研究 Agent 与多种能力
[:nexus/ask
 {:receiver :GPT4}
 ;; 定义 Agent 个性和专长
 [:system
  [:h1 "你是一位具有以下能力的 AI 研究助手："]
  [:ul
   [:li "数据分析和可视化"]
   [:li "文献综述和综合"]
   [:li "技术写作和文档编写"]]]
 
 ;; 集成嵌套组件完成复杂任务
 [:user
  [:h2 "研究项目：量子计算应用"]
  [:p "请分析以下信息："]
  
  ;; 文件分析组件
  [:file/analyze-code "src/quantum/algorithms.py"]
  
  ;; 网络搜索集成
  [:http/web "量子纠错的最新进展"]
  
  ;; 知识图谱集成
  [:db/domain-knowledge "量子计算"]
  
  ;; 输出格式说明
  [:p "请将你的回应格式化为结构化报告，包含："]
  [:ul
   [:li "执行摘要"]
   [:li "技术分析"]
   [:li "未来研究方向"]]]]
```

```clojure
;; 协作 Agent 模式
(let [;; 首先，执行代码分析 Agent
      code-analysis (render
                      [:nexus/ask
                       {:receiver :Gemini}
                       [:system "你是一位代码分析专家。"]
                       [:user [:file/analyze-code "src/core/architecture.clj"]]])
      
      ;; 然后，执行安全分析 Agent
      security-analysis (render
                          [:nexus/ask
                           {:receiver :GPT4}
                           [:system "你是一位安全审计专家。"]
                           [:user [:file/analyze-code "src/core/security.clj"]]])
      
      ;; 创建包含两个专业 Agent 结果的绑定
      bindings {'code-results code-analysis
                'security-results security-analysis}]
  
  ;; 最后，执行一个整合两种分析的总结 Agent
  (render bindings
          [:nexus/ask
           {:receiver :Claude}
           [:system "你是一位技术项目经理。"]
           [:user "基于这些分析创建一个综合改进计划："
            [:h3 "代码分析结果"]
            [:p 'code-results]
            [:h3 "安全分析结果"]
            [:p 'security-results]]]))
```

### 作为库使用

除了通过 DSL 向量语法使用 BlackFog 外，您还可以直接通过 Java/Clojure API 将其作为库引入您的项目中。以下是一些常见的使用场景：

#### 简洁的函数式 API

```clojure
(require '[blackfog.core :as bf])

;; 配置 LLM 服务
(bf/set-llm-config! :openai
  {:api/url "https://api.openai.com/v1"
   :api/sk "your-api-key-here"
   :api/model "gpt-4"
   :model/temperature 0.7})

;; 向 LLM 发送请求并获取回复
(bf/ask-llm :openai "解释量子计算的原理")

;; 文件操作
(bf/read-file "path/to/file.txt")
(bf/analyze-code "src/myapp/core.clj")

;; 知识图谱操作
(bf/create-node {:phrase/title "函数式编程"
                 :phrase/content "一种编程范式..."
                 :phrase/domain ["计算机科学" "编程范式"]})

;; HTTP 请求
(bf/http-get "https://api.example.com/data")
```

#### 使用绑定和变量

BlackFog 允许您在 DSL 表达式中使用变量绑定，这对于复用数据和构建动态表达式非常有用：

```clojure
(require '[blackfog.core :as bf])

;; 简单的变量绑定
(def bindings {'username "张三"
               'greeting "你好"})

;; 使用绑定渲染表达式
(bf/render bindings [:nexus/ask
                     {:receiver :default}
                     (str greeting ", " username "！欢迎使用 BlackFog！")])

;; 复杂的数据结构绑定
(def user-data {'user {:name "李四"
                       :role "管理员"
                       :projects ["项目A" "项目B"]}})

;; 使用复杂绑定
(bf/render user-data 
          [:db/create-node 
           {:phrase/title (str (:name user) "的资料")
            :phrase/content (str "角色: " (:role user))
            :phrase/tags (:projects user)}])

;; 自定义递归深度（适用于复杂表达式）
(bf/render {} [:complex/nested-expression ...] 0 200)
```

#### 扩展 DSL

您可以通过注册新的元素和组件来扩展 BlackFog 的 DSL 系统：

```clojure
(require '[blackfog.core :as bf])

;; 注册一个基础元素
(bf/register-element :my-app/greet
  (fn [name]
    (str "你好，" name "！欢迎使用 BlackFog！")))

;; 注册一个组件（会经过二次渲染）
(bf/register-component :my-app/user-card
  (fn [user]
    [:div
     [:h1 (str "用户: " (:name user))]
     [:p (str "邮箱: " (:email user))]
     [:p (str "角色: " (:role user))]]))

;; 使用注册的元素
(bf/render [:my-app/greet "张三"])
;; => "你好，张三！欢迎使用 BlackFog！"

;; 使用注册的组件
(bf/render [:my-app/user-card {:name "李四" :email "lisi@example.com" :role "管理员"}])
```

#### Java 互操作

从 Java 中使用 BlackFog：

```java
import blackfog.core;

public class BlackFogExample {
    public static void main(String[] args) {
        // 初始化 BlackFog
        blackfog.core.render(PersistentVector.create(Keyword.intern("http", "get"), "https://example.com"));
        
        // 使用便捷 API
        String result = (String) blackfog.core.ask_llm(Keyword.intern("openai"), "解释量子计算");
        System.out.println(result);
    }
}
```

#### 流式 LLM 响应

处理流式 LLM 响应：

```clojure
(require '[blackfog.core :as bf]
         '[clojure.core.async :refer [go-loop <!]])

(let [response-chan (bf/ask-llm-stream :openai "写一首关于编程的诗")]
  (go-loop []
    (when-let [chunk (<! response-chan)]
      (when (:success chunk) 
        (when-let [content (-> chunk :data :choices first :delta :content)]
          (print content)
          (flush)))
      (when-not (:done chunk)
        (recur)))))
```

有关更多示例和完整 API 文档，请参考 [API 文档](target/docs/index.html)。

### 文件分析

```clojure
;; 文件摘要分析
[:file/summary "path/to/file.txt"]

;; 代码分析
[:file/analyze-code "src/blackfog/dsl/core.clj"]
```

### 知识管理

```clojure
;; 创建知识节点
[:db/create-node {:phrase/title "量子计算"
                  :phrase/content "量子计算是利用量子力学现象进行信息处理的计算方式"
                  :phrase/domain ["物理学" "计算机科学"]
                  :phrase.related/words ["量子位" "量子纠缠"]}]

;; 添加关系
[:db/add-relation "量子计算" "经典计算" :related-to]

;; 可视化领域知识
[:db/visualize-domain "物理学"]
```

## 安装与配置

### 依赖管理

BlackFog 可以通过多种方式安装和使用：

#### Maven

```xml
<dependency>
  <groupId>io.github.yourusername</groupId>
  <artifactId>blackfog</artifactId>
  <version>0.1.0</version>
</dependency>
```

#### Gradle

```gradle
implementation 'io.github.yourusername:blackfog:0.1.0'
```

#### Clojure (deps.edn)

```clojure
io.github.yourusername/blackfog {:mvn/version "0.1.0"}
```

#### Leiningen

```clojure
[io.github.yourusername/blackfog "0.1.0"]
```

### 环境要求

- JDK 11+
- Clojure 1.11+
- Leiningen 或 deps.edn

### 快速开始

1. 克隆仓库
   ```bash
   git clone https://github.com/yourusername/blackfog.git
   cd blackfog
   ```

2. 安装依赖
   ```bash
   clojure -A:deps
   ```

3. 启动 REPL
   ```bash
   clojure -M:repl
   ```

4. 使用 REPL 运行示例
   ```clojure
   (require '[blackfog.core :as bf])
   
   ;; 运行简单的 DSL 示例
   (bf/render [:http/get "https://example.com/api"])
   ```

### 配置 LLM 服务

在 `resources/config/services.edn` 文件中配置你的 LLM 服务：

```clojure
{:openai {:api/url "https://api.openai.com/v1"
          :api/sk "your-api-key-here"
          :api/model "gpt-4"
          :model/temperature 0.7}
 
 :gemini {:api/url "https://generativelanguage.googleapis.com/v1beta"
          :api/sk "your-api-key-here"
          :api/model "gemini-pro"
          :model/temperature 0.2}}
```

## 开发规范

BlackFog 项目遵循以下开发规范：

1. **KISS 原则** - 保持简单直接的解决方案
2. **纯函数优先** - 优先使用纯函数，确保相同输入产生相同输出
3. **副作用分离** - 将副作用与业务逻辑分离
4. **优雅性** - 使用恰当的命名和一致的代码风格
5. **避免冗余** - 遵循 DRY 原则，提取共用逻辑
6. **错误处理** - 使用显式的错误处理而非异常捕获
7. **测试** - 为纯函数编写单元测试，使用基于属性的测试验证函数行为

## 扩展开发

BlackFog 设计为可扩展的系统，你可以通过以下方式添加新功能：

1. 添加新的 DSL 元素：

```clojure
(require '[blackfog.core :as bf])

;; 注册新的基础元素
(bf/register-element :my-namespace/my-function 
             (fn [& args] 
               ;; 实现你的功能
               ))
```

2. 添加新的组件：

```clojure
(require '[blackfog.core :as bf])

;; 注册新的组件
(bf/register-component :my-namespace/my-component
               (fn [& args]
                 ;; 返回一个新的 DSL 表达式
                 [:h1 "Generated Component"]))
```

## 贡献指南

欢迎对 BlackFog 项目做出贡献！请遵循以下步骤：

1. Fork 仓库
2. 创建功能分支 (`git checkout -b feature/amazing-feature`)
3. 提交更改 (`git commit -m 'Add some amazing feature'`)
4. 推送到分支 (`git push origin feature/amazing-feature`)
5. 创建 Pull Request

### 代码风格

- 遵循 Clojure 社区的代码风格指南
- 使用有意义的函数和变量命名
- 编写清晰的文档字符串
- 为公共 API 添加示例

### 发布流程

BlackFog 已作为 Maven 库发布。如果您想贡献并发布新版本，请阅读 [Maven 部署指南](docs/maven-deploy-guide.md) 了解详细信息。

基本的发布流程如下：

1. 准备好代码更改并确保测试通过
   ```bash
   clojure -M:test
   ```

2. 生成文档
   ```bash
   clojure -T:build docs
   ```

3. 在本地安装并测试
   ```bash
   clojure -T:build install
   ```

4. 发布新版本（需要正确的权限）
   ```bash
   clojure -T:build deploy
   ```

## 许可证

本项目采用 [Apache License 2.0](LICENSE)。

## 联系方式

- 项目维护者：neromous
- 邮箱：[neromous@outlook.com](mailto:neromous@outlook.com)
- Issues: [GitHub Issues](https://github.com/yourusername/blackfog/issues)
- qq群: 873610818

---

*BlackFog - 增强你的思维，扩展你的认知边界*