(ns blackfog.utils.data-formatter.edn
  (:require [clojure.edn :as edn]
            [clojure.pprint :as pprint]
            [clojure.walk :as walk])
  (:import [java.io PushbackReader StringReader]))

(defn text->edn [s]
  (let [reader  (PushbackReader. (StringReader. s))]
    (try (loop [items []]
           (let [item (edn/read {:eof nil} reader)]
             (if (= item nil)
               items
               (recur (conj items item)))))
         (catch Exception e (let [] (println "=====" e) [false])))))

(defn writer-edn [path obj]
  (let [text (with-out-str (clojure.pprint/pprint obj))]
    (spit path text :append true :encoding "utf-8") obj))

(defn read-edn [path]
  (let [text (slurp path :encoding "utf-8")]
    (text->edn text)))

(defn replace-symbols [form symbol-map]
  (clojure.walk/postwalk
   (fn [x] (if (and (symbol? x) (contains? symbol-map x))
             (get symbol-map x) x))
   form))

(defn load-edn-template [path symbol-map]
  (let [text (slurp path :encoding "utf-8")
        form (edn/read-string text)]
    (replace-symbols form symbol-map)))
