(ns blackfog.utils.data-formatter.json
  (:require [clj-http.client :as http]
            [clojure.string :as sr]
            [cheshire.core :as json]
            [clojure.edn :as edn]
            [clojure.java.io :as io])
  (:import [java.io PushbackReader StringReader]))

(defn load-jsonl [file-path]
  (with-open [rdr (clojure.java.io/reader file-path)]
    (into [] (map #(json/parse-string % keyword)
                  (line-seq rdr)))))

(defn load-jsonl-by-batch [file-path batch-size output-fn]
  (with-open [rdr (io/reader file-path)]
    (doseq [batch (partition-all batch-size
                                 (map #(json/parse-string % keyword)
                                      (line-seq rdr)))]
      (output-fn batch))))

(defn write-jsonl
  [file-path data  & {:keys [append]}]
  (cond
    (vector? data) (let [s (map json/generate-string data)
                         s (map #(str % "\n") s)
                         s (apply str s)]
                     (spit file-path s :append append))
    (map? data) (spit file-path (str (json/generate-string data) "\n")
                      :append append)
    :default  false))
