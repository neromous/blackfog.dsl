(ns build
  (:require [clojure.tools.build.api :as b]
            [clojure.string :as str]
            [clojure.java.shell :refer [sh]]))

(def lib 'io.github.yourusername/blackfog)
(def version (format "0.1.%s" (b/git-count-revs nil)))
(def class-dir "target/classes")
(def basis (b/create-basis {:project "deps.edn"}))
(def jar-file (format "target/%s-%s.jar" (name lib) version))
(def uber-file (format "target/%s-%s-standalone.jar" (name lib) version))
(def src-dirs ["src" "resources"])
(def docs-dir "target/docs")

(defn clean [_]
  (b/delete {:path "target"}))

(defn compile-java [_]
  (b/javac {:src-dirs ["java"]
            :class-dir class-dir
            :basis basis
            :javac-opts ["-source" "11" "-target" "11"]}))

(defn jar [_]
  (clean nil)
  (b/write-pom {:class-dir class-dir
                :lib lib
                :version version
                :basis basis
                :src-dirs src-dirs
                :scm {:tag (format "v%s" version)
                     :url "https://github.com/yourusername/blackfog"}
                :description "A flexible, composable framework providing a declarative DSL for complex knowledge processing and LLM integration"
                :url "https://github.com/yourusername/blackfog"
                :licenses [{"Apache-2.0" "https://www.apache.org/licenses/LICENSE-2.0"}]})
  (b/copy-dir {:src-dirs src-dirs
               :target-dir class-dir})
  (b/jar {:class-dir class-dir
          :jar-file jar-file}))

(defn uber [_]
  (clean nil)
  (b/copy-dir {:src-dirs src-dirs
               :target-dir class-dir})
  (b/compile-clj {:basis basis
                  :src-dirs src-dirs
                  :class-dir class-dir})
  (b/uber {:class-dir class-dir
           :uber-file uber-file
           :basis basis
           :main 'blackfog.core}))

(defn install [_]
  (jar nil)
  (b/install {:basis basis
              :lib lib
              :version version
              :jar-file jar-file
              :class-dir class-dir}))

(defn deploy [_]
  (jar nil)
  (b/process {:command-args ["mvn" "deploy:deploy-file"
                             (str "-Dfile=" jar-file)
                             (str "-DpomFile=" (str class-dir "/META-INF/maven/" (namespace lib) "/" (name lib) "/pom.xml"))
                             (str "-DrepositoryId=clojars")
                             (str "-Durl=https://clojars.org/repo")]}))

(defn docs [_]
  (println "生成 API 文档...")
  (let [env (assoc (into {} (System/getenv)) "VERSION" version)]
    (b/process {:command-args ["clojure" "scripts/generate-docs.clj"]
                :env env})))

(defn ci [_]
  (jar nil)
  (docs nil)
  (println "运行测试...")
  (b/process {:command-args ["clojure" "-M:test"]})) 