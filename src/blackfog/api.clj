(ns blackfog.api
  "BlackFog public API - Main entry point for library users.
   
   This namespace provides simplified access to BlackFog functionality
   and should be the primary interface for external consumers of the library."
  (:require [blackfog.dsl.core :as dsl]
            [blackfog.dsl.register]
            [blackfog.llm.client :as llm]
            [blackfog.db.core :as db]
            [clojure.core.async :as async]))

;; =================== Core DSL Functions ===================

(defn render
  "Renders a BlackFog DSL expression.
   
   This is the main entry point for evaluating DSL expressions.
   
   Supported arities:
   - [expr] - Renders the expression with default bindings
   - [bindings expr] - Renders the expression with custom bindings
   - [bindings expr depth max-depth] - Renders with custom bindings and depth limits
   
   Examples:
   ```clojure
   ;; Simple render
   (render [:http/get \"https://example.com/api\"])
   
   ;; Render with bindings
   (render {'api-url \"https://example.com/api\"} 
          [:http/get api-url])
   
   ;; Render with bindings and custom depth limits
   (render {'data {:user \"John\"}} 
          [:user/format data]
          0 50)
   ```"
  ([expr]
   (dsl/render expr))
  ([bindings expr]
   (dsl/render bindings expr))
  ([bindings expr depth max-depth]
   (dsl/render bindings expr depth max-depth)))

(defn register-element
  "Register a custom DSL element with the given keyword and handler function.
   
   Elements are the basic building blocks of BlackFog DSL.
   
   Examples:
   ```clojure
   (register-element :my-app/hello
                    (fn [name]
                      (str \"Hello, \" name \"!\")))
   
   ;; Usage:
   (render [:my-app/hello \"World\"]) ;; => \"Hello, World!\"
   ```"
  [key handler-fn]
  (dsl/reg-element key handler-fn))

(defn register-component
  "Register a custom DSL component with the given keyword and handler function.
   
   Components are higher-level abstractions that are rendered twice:
   first to generate a new expression, then to evaluate that expression.
   
   Examples:
   ```clojure
   (register-component :my-app/greeting
                      (fn [type name]
                        [:div
                         [:h1 (str type \", \" name \"!\")]
                         [:p \"Welcome to BlackFog\"]]))
   
   ;; Usage:
   (render [:my-app/greeting \"Hello\" \"World\"])
   ```"
  [key handler-fn]
  (dsl/reg-component key handler-fn))

;; =================== LLM Integration ===================

(defn ask-llm
  "Send a prompt to an LLM and get the response as a string.
   
   Parameters:
   - model-key: Keyword identifying the LLM service (e.g., :openai, :gemini)
   - prompt: The text prompt to send
   - system-prompt: Optional system instructions (defaults to nil)
   
   Examples:
   ```clojure
   (ask-llm :openai \"What is functional programming?\")
   
   (ask-llm :gemini 
            \"Explain quantum entanglement\"
            \"You are a quantum physics expert. Keep explanations simple.\")
   ```"
  ([model-key prompt]
   (ask-llm model-key prompt nil))
  ([model-key prompt system-prompt]
   (let [prompt-data (cond-> {:model model-key
                              :messages [{:role "user" :content prompt}]}
                             system-prompt
                             (update :messages #(into [{:role "system" :content system-prompt}] %)))
         response-chan (llm/async-post model-key "/chat/completions" prompt-data)]
     (-> (async/<!! (llm/stream->complete-response response-chan))
         :content))))

(defn ask-llm-stream
  "Send a prompt to an LLM and get the response as a channel that receives 
   content chunks as they arrive.
   
   Returns a core.async channel that will receive content chunks.
   
   Parameters:
   - model-key: Keyword identifying the LLM service (e.g., :openai, :gemini)
   - prompt: The text prompt to send
   - system-prompt: Optional system instructions (defaults to nil)
   
   Examples:
   ```clojure
   (let [response-chan (ask-llm-stream :openai \"Write a poem about clojure\")]
     (go-loop []
       (when-let [chunk (<! response-chan)]
         (print chunk)
         (flush)
         (recur))))
   ```"
  ([model-key prompt]
   (ask-llm-stream model-key prompt nil))
  ([model-key prompt system-prompt]
   (let [prompt-data (cond-> {:model model-key
                              :messages [{:role "user" :content prompt}]}
                             system-prompt
                             (update :messages #(into [{:role "system" :content system-prompt}] %)))]
     (llm/async-stream-post model-key "/chat/completions" prompt-data))))

;; =================== File Operations ===================

(defn read-file
  "Read the contents of a text file.
   
   Examples:
   ```clojure
   (read-file \"path/to/file.txt\")
   ```"
  [path]
  (render [:file/read-text path]))

(defn write-file
  "Write content to a text file.
   
   Examples:
   ```clojure
   (write-file \"path/to/file.txt\" \"Hello, world!\")
   ```"
  [path content]
  (render [:file/write-text path content]))

(defn analyze-file
  "Analyze a file and return a summary of its content.
   
   Examples:
   ```clojure
   (analyze-file \"path/to/code.clj\")
   ```"
  [path]
  (render [:file/summary path]))

(defn analyze-code
  "Analyze code file with detailed insights.
   
   Examples:
   ```clojure
   (analyze-code \"src/myapp/core.clj\")
   ```"
  [path]
  (render [:file/analyze-code path]))

;; =================== Knowledge Graph Operations ===================

(defn create-node
  "Create a knowledge node in the graph database.
   
   Examples:
   ```clojure
   (create-node {:phrase/title \"Functional Programming\"
                :phrase/content \"Programming paradigm treating computation as evaluation of mathematical functions\"
                :phrase/domain [\"Computer Science\" \"Programming Paradigms\"]})
   ```"
  [node-data]
  (render [:db/create-node node-data]))

(defn add-relation
  "Add a relationship between two nodes.
   
   Examples:
   ```clojure
   (add-relation \"Functional Programming\" \"Object-Oriented Programming\" :related-to)
   ```"
  [from-node to-node relation-type]
  (render [:db/add-relation from-node to-node relation-type]))

(defn visualize-domain
  "Generate a visualization of a knowledge domain.
   
   Examples:
   ```clojure
   (visualize-domain \"Computer Science\")
   ```"
  [domain]
  (render [:db/visualize-domain domain]))

;; =================== Project Analysis ===================

(defn analyze-project
  "Perform a comprehensive analysis of a project.
   
   Examples:
   ```clojure
   (analyze-project \"/path/to/project\")
   ```"
  [project-path]
  (render [:proj/health-assessment project-path]))

(defn analyze-dependencies
  "Analyze dependencies of a project.
   
   Examples:
   ```clojure
   (analyze-dependencies \"/path/to/project\")
   ```"
  [project-path]
  (render [:proj/dependency-analysis project-path]))

(defn suggest-refactoring
  "Suggest refactoring improvements for a project.
   
   Examples:
   ```clojure
   (suggest-refactoring \"/path/to/project\")
   ```"
  [project-path]
  (render [:proj/refactoring-suggestions project-path]))

;; =================== HTTP/Web Integration ===================

(defn http-get
  "Perform an HTTP GET request and return the response.
   
   Examples:
   ```clojure
   (http-get \"https://api.example.com/data\")
   ```"
  [url]
  (render [:http/get url]))

(defn http-post
  "Perform an HTTP POST request and return the response.
   
   Examples:
   ```clojure
   (http-post \"https://api.example.com/data\" {:name \"value\"})
   ```"
  [url data]
  (render [:http/post url data]))

(defn web-search
  "Perform a web search and return the results.
   
   Examples:
   ```clojure
   (web-search \"clojure dsl examples\")
   ```"
  [query]
  (render [:http/web query]))

;; =================== Configuration ===================

(defn set-llm-config!
  "Configure an LLM service with API keys and settings.
   
   Examples:
   ```clojure
   (set-llm-config! :openai {
     :api/url \"https://api.openai.com/v1\"
     :api/sk \"your-api-key\"
     :api/model \"gpt-4\"
     :model/temperature 0.7
   })
   ```"
  [service-key config]
  (alter-var-root #'llm/config update-in [:services service-key] (constantly config)))

;; =================== Convenience Utilities ===================

(defn dsl
  "DSL expression constructor with improved IDE support.
   This is an alternative to vector syntax with additional validation.
   
   Examples:
   ```clojure 
   (dsl :http/get \"https://example.com\")
   ;; equivalent to [:http/get \"https://example.com\"]
   
   (dsl :nexus/ask {:receiver :default} \"What is quantum computing?\")
   ;; equivalent to [:nexus/ask {:receiver :default} \"What is quantum computing?\"]
   ```"
  [tag & args]
  (apply vector tag args)) 