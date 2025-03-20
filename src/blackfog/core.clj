(ns blackfog.core
  "BlackFog Core - Main library entry point.
   
   This namespace re-exports the public API and serves as the primary
   entry point for users of the BlackFog library."
  (:require [blackfog.api :as api]
            [blackfog.dsl.register] ;; Ensure all DSL elements are registered
            [blackfog.llm.client]))

;; Re-export public API functions with proper arities
(defn render
  "Renders a BlackFog DSL expression.
   
   Supported arities:
   - [expr] - Renders the expression with default bindings
   - [bindings expr] - Renders the expression with custom bindings
   - [bindings expr depth max-depth] - Renders with custom bindings and depth limits
   
   See blackfog.api/render for full documentation."
  ([expr]
   (api/render expr))
  ([bindings expr]
   (api/render bindings expr))
  ([bindings expr depth max-depth]
   (api/render bindings expr depth max-depth)))

(def register-element api/register-element)
(def register-component api/register-component)

;; LLM Functions
(def ask-llm api/ask-llm)
(def ask-llm-stream api/ask-llm-stream)

;; Note: Unlike other functions, this is a direct var reference due to how we
;; modify configuration - we use alter-var-root since config is not an atom
(def set-llm-config! api/set-llm-config!)

;; File Operations
(def read-file api/read-file)
(def write-file api/write-file)
(def analyze-file api/analyze-file)
(def analyze-code api/analyze-code)

;; Knowledge Graph
(def create-node api/create-node)
(def add-relation api/add-relation)
(def visualize-domain api/visualize-domain)

;; Project Analysis
(def analyze-project api/analyze-project)
(def analyze-dependencies api/analyze-dependencies)
(def suggest-refactoring api/suggest-refactoring)

;; HTTP/Web
(def http-get api/http-get)
(def http-post api/http-post)
(def web-search api/web-search)

;; Utilities
(def dsl api/dsl)

;; Re-export all functions with metadata for discovery
(doseq [[sym var] (ns-publics 'blackfog.api)]
  (intern *ns* sym var))

(defn usage-examples
  "Returns a map of usage examples for BlackFog.
   
   This function is meant to be used in a REPL to explore 
   the capabilities of the library."
  []
  {:dsl-examples
   {:http-request
    "(render [:http/get \"https://example.com/api\"])"
    
    :llm-query
    "(render [:nexus/ask {:receiver :default} \"What is quantum computing?\"])"
    
    :file-analysis
    "(render [:file/analyze-code \"src/myapp/core.clj\"])"
    
    :knowledge-graph
    "(render [:db/create-node {:phrase/title \"Functional Programming\"
                               :phrase/content \"Programming paradigm...\"
                               :phrase/domain [\"Computer Science\"]}])"
    
    :project-analysis
    "(render [:proj/health-assessment \"/path/to/project\"])"}
   
   :function-examples
   {:llm-query
    "(ask-llm :openai \"Explain monads in simple terms\")"
    
    :file-operations
    "(analyze-code \"src/myapp/core.clj\")"
    
    :knowledge-graph
    "(create-node {:phrase/title \"Functional Programming\"
                   :phrase/content \"Programming paradigm...\"
                   :phrase/domain [\"Computer Science\"]})"
    
    :configuration
    "(set-llm-config! :openai {:api/url \"https://api.openai.com/v1\"
                               :api/sk \"your-api-key\"
                               :api/model \"gpt-4\"
                               :model/temperature 0.7})"}})

(defn config-example
  "Returns an example configuration map for BlackFog.
   
   This function is meant to be used as a template for 
   setting up the library."
  []
  {:llm-services
   {:openai {:api/url "https://api.openai.com/v1"
             :api/sk "your-openai-api-key-here"
             :api/model "gpt-4"
             :model/temperature 0.7}
    
    :gemini {:api/url "https://generativelanguage.googleapis.com/v1beta"
             :api/sk "your-gemini-api-key-here"
             :api/model "gemini-pro"
             :model/temperature 0.5}}})

(defn -main
  "Application entry point when BlackFog is used as a standalone app.
   
   For library use, ignore this function and use the API directly."
  [& args]
  (println "BlackFog DSL Framework")
  (println "Version: 0.1.0")
  (println "Usage as library: (require '[blackfog.core :as blackfog])")
  (println "See documentation for more information: https://github.com/yourusername/blackfog")
  
  ;; If arguments are provided, treat the first as a DSL expression file
  (when (seq args)
    (let [file (first args)]
      (println "Evaluating DSL file:" file)
      (try
        (let [expr (read-string (slurp file))]
          (println "Result:")
          (println (render expr)))
        (catch Exception e
          (println "Error evaluating DSL file:" (.getMessage e)))))))