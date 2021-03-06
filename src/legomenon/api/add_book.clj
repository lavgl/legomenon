(ns legomenon.api.add-book
  (:refer-clojure :exclude [number?])
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [mount.core :as mount]
            [pantomime.extract :as extract]
            [dk.simongray.datalinguist :as nlp]

            [legomenon.db :as db]
            [legomenon.utils :as utils]))


(defn insert-book-q [book]
  {:insert-into [:books]
   :values      [book]})


(defn book->db [{:keys [filename text]}]
  {:id       (utils/md5 text)
   :filename filename
   :text     text})


(defn parse-book [{:keys [filename tempfile]}]
  (let [text (->> (io/input-stream tempfile)
                  extract/parse
                  :text)]
    {:filename filename
     :text     text}))


;; lemmas extraction


(mount/defstate sentence-splitter
  :start (nlp/->pipeline {:annotators ["ssplit"]}))


(mount/defstate nlp
  :start (nlp/->pipeline {:annotators ["lemma"]}))


(mount/defstate words
  :start (->> (slurp (io/resource "words"))
              (str/split-lines)
              set))


(defn word-exists? [s]
  (contains? words s))


(defn remove-explicit-line-breaks [text]
  (str/replace text "-\n" ""))


(defn split-sentences [text]
  (->> text
       sentence-splitter
       nlp/sentences
       nlp/text))


(defn lemma-frequencies [text]
  (let [xf (comp
             (map nlp)
             (map nlp/tokens)
             (mapcat nlp/recur-datafy)
             (map :lemma)
             (filter word-exists?)
             (map str/lower-case))]
    (->> (remove-explicit-line-breaks text)
         (split-sentences)
         (transduce xf conj)
         frequencies
         (sort-by second >))))


(defn insert-lemmas-q [values]
  {:insert-into [:lemma_count]
   :values      values})


(defn lemmas->db [lemmas-count book-id]
  (letfn [(map-fn [[lemma lemma-count]]
            {:lemma   lemma
             :count   lemma-count
             :book_id book-id})]
    (map map-fn lemmas-count)))


;; add book api


(defn handler [req]
  (try
    (let [file         (-> req :params :file)
          book         (parse-book file)
          book-db      (book->db book)
          lemmas-count (lemma-frequencies (:text book-db))
          lemmas-db    (lemmas->db lemmas-count (:id book-db))]
      (db/execute db/conn (insert-book-q book-db))
      (db/execute db/conn (insert-lemmas-q lemmas-db))
      {:status  301
       :headers {"Location" "/"}})
    (catch Exception e
      {:status  301
       :headers {"Location" "/?message=book-duplicate"}})))
