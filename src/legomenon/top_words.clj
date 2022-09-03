(ns legomenon.top-words
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [mount.core :as mount]))


(defn parse-line [line]
  (let [word+occurence (str/split line #" ")]
    (first word+occurence)))


(defn words-seq []
  (->> (io/resource "en_50k.txt")
       (io/reader)
       line-seq
       (map parse-line)))


(time
  (->> (words-seq)
       (set)))


(defn build-index []
  (let [words  (words-seq)
        top4k  (->> words
                    (take 4000)
                    set)
        top10k (->> words
                    (take 10000)
                    set)]
    {:top4k  top4k
     :top10k top10k}))


(mount/defstate index
  :start (build-index))


(defn in-4k? [word]
  (contains? (:top4k index) word))


(defn in-10k? [word]
  (contains? (:top10k index) word))
