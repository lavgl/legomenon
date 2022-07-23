(ns legomenon.scripts.dictionary
  (:require [clojure.java.io :as io]
            [clojure.string :as str]

            [dk.simongray.datalinguist :as nlp]

            ))


;; 1. get dict text
;; 2. split on chunks
;; 3. process chunks
;; 3.1. remove words where lemma is not equal to word itself
;; 3.2. remove all except O
;; 3.3. remove if there numbers or punctuations
;; 4. save new file


(def allowed-entity-tags #{"O" "TIME" "SET" "DATE" "TITLE" "DURATION"})
(def disallowed-parts-of-speech #{"GW" "FW"})


(defn only-lemmas [{:keys [lemma original-text]}]
  (= lemma original-text))


(defn only-allowed-pos [{:keys [part-of-speech]}]
  (not (contains? disallowed-parts-of-speech part-of-speech)))


(defn only-allowed-entity-tags [{:keys [coarse-named-entity-tag]}]
  (contains? allowed-entity-tags coarse-named-entity-tag))


(defn pipeline [text]
  (let [nlp    (nlp/->pipeline {:annotators ["lemma" "ner"]})
        chunks (->> text
                    (str/split-lines)
                    (remove #(re-seq #"\W" %))
                    (partition-all 500))
        xf     (comp
                 (map-indexed (fn [i b] (println i "/" (count chunks)) b))
                 (map #(str/join " " %))
                 (map nlp)
                 (map nlp/tokens)
                 (mapcat nlp/recur-datafy)
                 (filter only-lemmas)
                 (filter only-allowed-pos)
                 (filter only-allowed-entity-tags)
                 (map :text))]
    (->> chunks
         (transduce xf conj)
         (str/join "\n"))))


(defn process []
  (let [dict   (slurp (io/file "/Users/v.homonov/Downloads/wlist_all/wlist_match6.txt"))
        result (pipeline dict)]
    (spit "words" result)
    (println "done!")))


(comment
  (process))
