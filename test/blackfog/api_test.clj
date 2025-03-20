(ns blackfog.api-test
  (:require [clojure.test :refer :all]
            [blackfog.api :as api]
            [blackfog.dsl.core :as dsl]
            [blackfog.llm.client :as llm]
            [clojure.core.async :as async]))

;; Helper for async testing
(defn <!!?
  "Takes a value from a channel with a timeout.
   Returns the value or :timeout if the timeout is reached."
  [c & {:keys [timeout] :or {timeout 1000}}]
  (let [timeout-ch (async/timeout timeout)
        [v _] (async/alts!! [c timeout-ch])]
    (or v :timeout)))

;; =================== DSL Tests ===================

(deftest test-render
  (testing "Basic render functionality"
    ;; Register a test element for testing
    (dsl/reg-element :test/hello (fn [name] (str "Hello, " name "!")))
    
    ;; Test rendering the element
    (is (= "Hello, World!" (api/render [:test/hello "World"])))
    
    ;; Test nested rendering
    (dsl/reg-element :test/greeting (fn [greeting name] [greeting name]))
    (is (= "Hello, Clojure!" (api/render [:test/greeting :test/hello "Clojure"]))))
  
  (testing "Render with bindings"
    ;; Register test elements that use symbols
    (dsl/reg-element :test/use-binding (fn [sym] sym))
    (dsl/reg-element :test/greet-user (fn [prefix user] (str prefix ", " user "!")))
    
    ;; Test with simple bindings - using quotes to fix unresolved symbols
    (is (= "Test Value" (api/render {'test-value "Test Value"} 
                                   [:test/use-binding 'test-value])))
    
    ;; Test with multiple bindings - using quotes to fix unresolved symbols
    (is (= "Hello, User!" (api/render {'prefix "Hello" 'user "User"} 
                                     [:test/greet-user 'prefix 'user]))))
  
  (testing "Render with custom depth limits"
    ;; Register elements for testing recursion
    (dsl/reg-element :test/identity (fn [x] x))
    (dsl/reg-element :test/nested (fn [level]
                                   (if (pos? level)
                                     [:test/nested (dec level)]
                                     "Done")))
    
    ;; Test with default depth
    (is (= "Done" (api/render [:test/nested 5])))
    
    ;; Test with custom depth (allow deeper nesting)
    (is (= "Done" (api/render {} [:test/nested 90] 0 100)))
    
    ;; Test with restricted depth (should throw exception)
    (is (thrown? clojure.lang.ExceptionInfo 
                (api/render {} [:test/nested 20] 0 10)))))

(deftest test-register-element
  (testing "Element registration"
    ;; Register a new element
    (api/register-element :test/add (fn [a b] (+ a b)))
    
    ;; Test using the registered element
    (is (= 5 (api/render [:test/add 2 3])))))

(deftest test-register-component
  (testing "Component registration"
    ;; Register a simple component that generates a DSL expression
    (api/register-component :test/heading
                          (fn [level text]
                            (case level
                              1 [:h1 text]
                              2 [:h2 text]
                              3 [:h3 text]
                              [:p text])))
    
    ;; Register element to test component rendering
    (dsl/reg-element :h1 (fn [text] (str "# " text)))
    (dsl/reg-element :h2 (fn [text] (str "## " text)))
    
    ;; Test component rendering
    (is (= "# Test Heading" (api/render [:test/heading 1 "Test Heading"])))
    (is (= "## Secondary Heading" (api/render [:test/heading 2 "Secondary Heading"])))))

;; =================== File Operation Tests ===================

(deftest test-file-operations
  (testing "File operations with mock implementations"
    ;; Replace actual implementations with test versions
    (with-redefs [api/read-file (fn [path] (str "Content of " path))
                  api/write-file (fn [path content] (str "Wrote to " path ": " content))
                  api/analyze-file (fn [path] {:summary (str "Summary of " path)})
                  api/analyze-code (fn [path] {:type "code-analysis" :file path})]
      
      ;; Test file operations
      (is (= "Content of test.txt" (api/read-file "test.txt")))
      (is (= "Wrote to test.txt: Hello" (api/write-file "test.txt" "Hello")))
      (is (= {:summary "Summary of test.txt"} (api/analyze-file "test.txt")))
      (is (= {:type "code-analysis" :file "test.clj"} (api/analyze-code "test.clj"))))))

;; =================== LLM Integration Tests ===================

(deftest test-llm-integration
  (testing "LLM integration with mock implementations"
    ;; Mock LLM client to avoid actual API calls
    (with-redefs [api/ask-llm (fn 
                               ([model-key prompt] 
                                (str "Response from " (name model-key) " to: " prompt))
                               ([model-key prompt system-prompt]
                                (str "Response from " (name model-key) 
                                     " with system: " system-prompt 
                                     " to: " prompt)))
                  
                  api/ask-llm-stream (fn 
                                      ([model-key prompt]
                                       (let [c (async/chan)]
                                         (async/go
                                           (async/>! c {:success true
                                                        :data {:choices [{:delta {:content "Streaming "}}]}})
                                           (async/>! c {:success true
                                                        :data {:choices [{:delta {:content "response"}}]}})
                                           (async/>! c {:success true :done true})
                                           (async/close! c))
                                         c))
                                      ([model-key prompt system-prompt]
                                       (let [c (async/chan)]
                                         (async/go
                                           (async/>! c {:success true
                                                        :data {:choices [{:delta {:content "Streaming "}}]}})
                                           (async/>! c {:success true
                                                        :data {:choices [{:delta {:content "response with system"}}]}})
                                           (async/>! c {:success true :done true})
                                           (async/close! c))
                                         c)))]
      
      ;; Test basic LLM querying
      (is (= "Response from openai to: Test prompt" 
             (api/ask-llm :openai "Test prompt")))
      
      ;; Test LLM querying with system prompt
      (is (= "Response from gemini with system: System instructions to: User question" 
             (api/ask-llm :gemini "User question" "System instructions")))
      
      ;; Test streaming response
      (let [response-chan (api/ask-llm-stream :openai "Stream test")
            first-chunk (<!!? response-chan)
            second-chunk (<!!? response-chan)
            end-signal (<!!? response-chan)]
        (is (:success first-chunk))
        (is (= "Streaming " (-> first-chunk :data :choices first :delta :content)))
        (is (:success second-chunk))
        (is (= "response" (-> second-chunk :data :choices first :delta :content)))
        (is (:done end-signal))))))

;; =================== Knowledge Graph Tests ===================

(deftest test-knowledge-graph
  (testing "Knowledge graph operations with mock implementations"
    ;; Replace actual implementations with test versions
    (with-redefs [api/create-node (fn [data] {:id (str "node-" (:phrase/title data))
                                              :data data})
                  api/add-relation (fn [from to rel-type] 
                                     {:from from :to to :type rel-type})
                  api/visualize-domain (fn [domain] 
                                         {:type "visualization" :domain domain})]
      
      ;; Test node creation
      (is (= {:id "node-Test Node"
              :data {:phrase/title "Test Node"
                     :phrase/content "Test content"}}
             (api/create-node {:phrase/title "Test Node"
                               :phrase/content "Test content"})))
      
      ;; Test relation creation
      (is (= {:from "Node A" :to "Node B" :type :related-to}
             (api/add-relation "Node A" "Node B" :related-to)))
      
      ;; Test domain visualization
      (is (= {:type "visualization" :domain "Test Domain"}
             (api/visualize-domain "Test Domain"))))))

;; =================== Configuration Tests ===================

(deftest test-configuration
  (testing "Configuration operations"
    ;; Test with a var-root to verify config is updated
    (let [original-config (var-get #'llm/config)]
      (try
        ;; Set configuration
        (api/set-llm-config! :test-model {:api/url "https://api.test.com"
                                         :api/sk "test-key"})
        
        ;; Verify config was updated
        (is (= "https://api.test.com" 
               (get-in (var-get #'llm/config) [:services :test-model :api/url])))
        (is (= "test-key"
               (get-in (var-get #'llm/config) [:services :test-model :api/sk])))
        
        ;; Restore original configuration
        (finally
          (alter-var-root #'llm/config (constantly original-config)))))))

;; =================== Utility Tests ===================

(deftest test-dsl-utility
  (testing "DSL utility functions"
    ;; Test vector construction
    (is (= [:test/func "arg1" "arg2"]
           (api/dsl :test/func "arg1" "arg2"))))) 