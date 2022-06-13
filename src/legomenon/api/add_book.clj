(ns legomenon.api.add-book
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
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


;; NOTE: is it ok to keep it in file scope instead of wrap in state?
(def nlp-pipeline (nlp/->pipeline {:annotators ["lemma"]}))


(defn punctuation? [s]
  (boolean (re-matches #"^\W$" s)))


(defn number? [s]
  (boolean (re-matches #"^\d*$" s)))


(defn phone? [s]
  (boolean (re-matches #"^\+\d+$" s)))


(defn email? [s]
  (str/includes? s "@"))


(defn contacts? [s]
  (or (phone? s) (email? s)))


(defn url? [s]
  (str/starts-with? s "http"))


(defn lemma-frequencies [text]
  (->> (nlp-pipeline text)
       nlp/tokens
       nlp/lemma
       nlp/recur-datafy
       (remove punctuation?)
       (remove number?)
       (remove contacts?)
       (remove url?)
       (map str/lower-case)
       frequencies
       (sort-by second >)))


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
