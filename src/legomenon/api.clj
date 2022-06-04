(ns legomenon.api
  (:refer-clojure :exclude [number?])
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [dk.simongray.datalinguist :as nlp]
            [pantomime.extract :as extract]
            [hiccup.core :refer [html]]

            [legomenon.db :as db]
            [legomenon.utils :as utils]
            [legomenon.fe :as fe]))


;; books text extraction


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


(defn add-book [req]
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


;; mark word as trash api


(defn word-q [word-id]
  {:select [:id :lemma :count]
   :from   [:lemma_count]
   :where  [:= :id word-id]})


(defn add-word-to-trash-list-q [word]
  {:insert-or-ignore-into :trash_words
   :values                [{:word word}]})


(defn mark-word-as-trash [req]
  (let [word-id (-> req :path-params :id)
        word    (db/one db/conn (word-q word-id))]
    (if (some? word)
      (do
        (db/execute db/conn (add-word-to-trash-list-q (:lemma word)))
        {:status 200
         :body   (html (fe/render-trash-row word))})
      {:status 404})))


;; mark work as known api


(defn add-word-to-known-list-q [word]
  {:insert-or-ignore-into :known_words
   :values                [{:word word}]})


(defn mark-word-as-known [req]
  (let [word-id (-> req :path-params :id)
        word    (db/one db/conn (word-q word-id))]
    (if (some? word)
      (do
        (db/execute db/conn (add-word-to-known-list-q (:lemma word)))
        {:status 200
         :body   (html (fe/render-known-row word))})
      {:status 404})))
