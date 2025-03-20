# BlackFog

BlackFog is a powerful framework providing a domain-specific language (DSL) for complex knowledge processing and LLM integration.

## Project Overview

BlackFog is a flexible, composable knowledge processing framework that provides a declarative domain-specific language (DSL) for expressing and processing complex knowledge structures. Through vector-based syntax and component-based design, BlackFog simplifies the process of knowledge extraction, transformation, and interaction, making it especially suitable for integration with Large Language Models (LLMs).

Core design principles:
- **Declarative**: Use concise vector syntax to describe complex operations
- **Composable**: Support functional composition for building complex workflows
- **Extensible**: Modular design for easy extension of functionality and integration with external services

### Project Goals

BlackFog aims to implement a truly reactive, declarative programming paradigm to help developers build more robust and flexible knowledge processing systems:

1. **Declarative Programming in Practice**: Let developers focus on describing "what to do" rather than "how to do it" through vector-based DSL, reducing cognitive load
2. **Reactive Data Flows**: Leverage core.async for reactive programming support, enabling asynchronous processing and streaming of data
3. **Functional Immutability**: Follow functional programming principles, reduce side effects, and improve code predictability and testability
4. **Enhanced LLM Capabilities**: Enhance reasoning and generation capabilities of large language models through structured knowledge representation

These goals make BlackFog an ideal tool for:
- Knowledge-intensive application development
- Complex data flow processing systems
- LLM-powered intelligent applications
- Enterprise applications requiring flexible extension

## System Architecture

BlackFog consists of the following core components:

1. **DSL Core** - Provides a flexible domain-specific language for expressing and processing complex knowledge structures
   - Element Registry System
   - Component System
   - Render Engine
2. **LLM Integration** - Integration with large language models for intelligent analysis and generation
   - Multi-model support (OpenAI, Gemini, etc.)
   - Streaming response processing
   - Conversation history management
3. **Functional Modules** - Provides rich functional modules, including:
   - File and folder operations
   - Knowledge graph management
   - HTTP request processing
   - Media file processing

## Core Features

### DSL System

BlackFog's DSL system allows expressing complex operations in a declarative manner, with key features including:

- Vector-based syntax that is easy to read and write
- Support for component-based and functional composition
- Flexible rendering and transformation mechanisms

```clojure
;; DSL Example - HTTP Request
[:http/get "https://example.com/api/data"]

;; DSL Example - User Q&A
[:nexus/ask
 {:receiver :default}
 "Please explain the basic principles of quantum computing"]
```

### File and Folder Management

The system provides comprehensive file and folder operation capabilities:

- File reading, writing, moving, and deletion
- Folder structure analysis and visualization
- Intelligent file content analysis and summarization

```clojure
;; File reading example
[:file/read-text "path/to/file.txt"]

;; Folder structure analysis
[:folder/structure "path/to/project"]
```

### Knowledge Graph

BlackFog includes a graph database system for building and querying knowledge graphs:

- Create and manage knowledge nodes
- Establish relationships between nodes
- Organize and visualize knowledge by domain

```clojure
;; Create a knowledge node
[:db/create-node {:phrase/title "Quantum Computing"
                  :phrase/content "Quantum computing is a type of computation that uses quantum phenomena for information processing"
                  :phrase/domain ["Physics" "Computer Science"]
                  :phrase.related/words ["Qubit" "Quantum Entanglement"]}]
```

### Project Analysis

Provides powerful project analysis tools:

- Code quality assessment
- Technical debt identification
- Documentation quality analysis
- Refactoring suggestions

```clojure
;; Project health assessment
[:proj/health-assessment "path/to/project"]

;; Code quality analysis
[:proj/code-quality "path/to/code"]
```

## Usage Examples

### Basic DSL Usage

```clojure
;; HTTP Request
[:http/get "https://en.wikipedia.org/wiki/Quantum_computing"]

;; Simple user Q&A
[:nexus/ask
 {:receiver :default}
 "Please explain the basic principles of quantum computing"]

;; Dialog with system prompt
[:nexus/ask
 {:receiver :Gemini}
 [:system [:h1 "You are a quantum physics expert. Please answer questions in simple terms."]
           [:ul :ordered 
            [:li "You have experience in Clojure programming"]
            [:li "Your code skills are excellent, following the 'KISS' principle"]]]
  [:prompt "What is quantum entanglement?"]]
```

### Using as a Library

Besides using BlackFog through the DSL vector syntax, you can also incorporate it into your projects as a Java/Clojure API. Here are some common use cases:

#### Concise Functional API

```clojure
(require '[blackfog.core :as bf])

;; Configure LLM service
(bf/set-llm-config! :openai
  {:api/url "https://api.openai.com/v1"
   :api/sk "your-api-key-here"
   :api/model "gpt-4"
   :model/temperature 0.7})

;; Send a request to LLM and get a response
(bf/ask-llm :openai "Explain the principles of quantum computing")

;; File operations
(bf/read-file "path/to/file.txt")
(bf/analyze-code "src/myapp/core.clj")

;; Knowledge graph operations
(bf/create-node {:phrase/title "Functional Programming"
                 :phrase/content "A programming paradigm..."
                 :phrase/domain ["Computer Science" "Programming Paradigms"]})

;; HTTP requests
(bf/http-get "https://api.example.com/data")
```

#### Using Bindings and Variables

BlackFog allows you to use variable bindings in DSL expressions, which is useful for reusing data and building dynamic expressions:

```clojure
(require '[blackfog.core :as bf])

;; Simple variable binding
(def bindings {'username "John"
               'greeting "Hello"})

;; Render expression with bindings
(bf/render bindings [:nexus/ask
                     {:receiver :default}
                     (str greeting ", " username "! Welcome to BlackFog!")])

;; Complex data structure binding
(def user-data {'user {:name "Alice"
                       :role "Administrator"
                       :projects ["Project A" "Project B"]}})

;; Using complex bindings
(bf/render user-data 
          [:db/create-node 
           {:phrase/title (str (:name user) "'s Profile")
            :phrase/content (str "Role: " (:role user))
            :phrase/tags (:projects user)}])

;; Custom recursion depth (for complex expressions)
(bf/render {} [:complex/nested-expression ...] 0 200)
```

#### Extending the DSL

You can extend BlackFog's DSL system by registering new elements and components:

```clojure
(require '[blackfog.core :as bf])

;; Register a basic element
(bf/register-element :my-app/greet
  (fn [name]
    (str "Hello, " name "! Welcome to BlackFog!")))

;; Register a component (rendered twice)
(bf/register-component :my-app/user-card
  (fn [user]
    [:div
     [:h1 (str "User: " (:name user))]
     [:p (str "Email: " (:email user))]
     [:p (str "Role: " (:role user))]]))

;; Use the registered element
(bf/render [:my-app/greet "John"])
;; => "Hello, John! Welcome to BlackFog!"

;; Use the registered component
(bf/render [:my-app/user-card {:name "Alice" :email "alice@example.com" :role "Administrator"}])
```

#### Java Interoperability

Using BlackFog from Java:

```java
import blackfog.core;

public class BlackFogExample {
    public static void main(String[] args) {
        // Initialize BlackFog
        blackfog.core.render(PersistentVector.create(Keyword.intern("http", "get"), "https://example.com"));
        
        // Use the convenient API
        String result = (String) blackfog.core.ask_llm(Keyword.intern("openai"), "Explain quantum computing");
        System.out.println(result);
    }
}
```

#### Streaming LLM Responses

Processing streaming LLM responses:

```clojure
(require '[blackfog.core :as bf]
         '[clojure.core.async :refer [go-loop <!]])

(let [response-chan (bf/ask-llm-stream :openai "Write a poem about programming")]
  (go-loop []
    (when-let [chunk (<! response-chan)]
      (when (:success chunk) 
        (when-let [content (-> chunk :data :choices first :delta :content)]
          (print content)
          (flush)))
      (when-not (:done chunk)
        (recur)))))
```

For more examples and complete API documentation, please refer to the [API Documentation](target/docs/index.html).

### File Analysis

```clojure
;; File summary analysis
[:file/summary "path/to/file.txt"]

;; Code analysis
[:file/analyze-code "src/blackfog/dsl/core.clj"]
```

### Knowledge Management

```clojure
;; Create a knowledge node
[:db/create-node {:phrase/title "Quantum Computing"
                  :phrase/content "Quantum computing is a type of computation that uses quantum phenomena for information processing"
                  :phrase/domain ["Physics" "Computer Science"]
                  :phrase.related/words ["Qubit" "Quantum Entanglement"]}]

;; Add a relationship
[:db/add-relation "Quantum Computing" "Classical Computing" :related-to]

;; Visualize domain knowledge
[:db/visualize-domain "Physics"]
```

## Installation and Configuration

### Dependency Management

BlackFog can be installed and used in various ways:

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

### Requirements

- JDK 11+
- Clojure 1.11+
- Leiningen or deps.edn

### Quick Start

1. Clone the repository
   ```bash
   git clone https://github.com/yourusername/blackfog.git
   cd blackfog
   ```

2. Install dependencies
   ```bash
   clojure -A:deps
   ```

3. Start the REPL
   ```bash
   clojure -M:repl
   ```

4. Run examples in the REPL
   ```clojure
   (require '[blackfog.core :as bf])
   
   ;; Run a simple DSL example
   (bf/render [:http/get "https://example.com/api"])
   ```

### Configuring LLM Services

Configure your LLM services in the `resources/config/services.edn` file:

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

## Development Standards

BlackFog follows these development standards:

1. **KISS Principle** - Keep solutions simple and direct
2. **Pure Functions First** - Prioritize pure functions that ensure the same input produces the same output
3. **Separation of Side Effects** - Separate side effects from business logic
4. **Elegance** - Use appropriate naming and consistent code style
5. **Avoid Redundancy** - Follow the DRY principle and extract common logic
6. **Error Handling** - Use explicit error handling rather than exception catching
7. **Testing** - Write unit tests for pure functions, use property-based tests to verify function behavior

## Extension Development

BlackFog is designed as an extensible system. You can add new functionality through:

1. Adding new DSL elements:

```clojure
(require '[blackfog.core :as bf])

;; Register a new basic element
(bf/register-element :my-namespace/my-function 
             (fn [& args] 
               ;; Implement your functionality
               ))
```

2. Adding new components:

```clojure
(require '[blackfog.core :as bf])

;; Register a new component
(bf/register-component :my-namespace/my-component
               (fn [& args]
                 ;; Return a new DSL expression
                 [:h1 "Generated Component"]))
```

## Contribution Guidelines

Contributions to BlackFog are welcome! Please follow these steps:

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add some amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Create a Pull Request

### Code Style

- Follow Clojure community code style guidelines
- Use meaningful function and variable names
- Write clear documentation strings
- Add examples for public APIs

### Release Process

BlackFog is published as a Maven library. If you want to contribute and publish a new version, please read the [Maven Deployment Guide](docs/maven-deploy-guide.md) for details.

The basic release process is as follows:

1. Prepare code changes and ensure tests pass
   ```bash
   clojure -M:test
   ```

2. Generate documentation
   ```bash
   clojure -T:build docs
   ```

3. Install and test locally
   ```bash
   clojure -T:build install
   ```

4. Publish a new version (requires proper permissions)
   ```bash
   clojure -T:build deploy
   ```

## License

This project is licensed under the [Apache License 2.0](LICENSE).

## Contact

- Project Maintainer: Mr. Zhang
- Email: [example@example.com](mailto:example@example.com)
- Issues: [GitHub Issues](https://github.com/yourusername/blackfog/issues)

---

*BlackFog - Enhance your thinking, expand your cognitive boundaries* 