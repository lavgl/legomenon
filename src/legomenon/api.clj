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


;; mark word as known/trash api


(defn word-q [word-id]
  {:select [:id :lemma :count :book_id]
   :from   [:lemma_count]
   :where  [:= :id word-id]})


(defn add-word-to-list-q [list word]
  (assert list)
  {:insert-or-replace-into :my_words
   :values                 [{:word       word
                             :list       (name list)
                             :deleted_at nil}]})


(defn remove-word-from-lists-q [word]
  {:update :my_words
   :set    {:deleted_at :current_timestamp
            :list       nil}
   :where  [:= :word word]})


(defn update-book-used-at-q [book-id]
  {:update :books
   :set    {:used_at :current_timestamp}
   :where  [:= :id book-id]})


(defn operate-on-word [req]
  (let [event-type (-> req :params :event)
        direction  (-> req :params :direction)
        key        (-> req :params :key)
        word-id    (-> req :params :id)
        keyup?     (= event-type "keyup")
        swipe?     (= event-type "swipe")
        ;; TODO: move to deps.edn and use blet
        word       (db/one db/conn (word-q word-id))]
    (cond
      (and keyup? (some? word) (= key "k"))
      (do
        (db/execute db/conn (add-word-to-list-q :known (:lemma word)))
        (db/execute db/conn (update-book-used-at-q (:book_id word)))
        {:status 200
         :body   (html (fe/render-known-row word))})

      (and keyup? (some? word) (= key "t"))
      (do
        (db/execute db/conn (add-word-to-list-q :trash (:lemma word)))
        (db/execute db/conn (update-book-used-at-q (:book_id word)))
        {:status 200
         :body   (html (fe/render-trash-row word))})

      (and keyup? (some? word) (= key "m"))
      (do
        (db/execute db/conn (add-word-to-list-q :memo (:lemma word)))
        (db/execute db/conn (update-book-used-at-q (:book_id word)))
        {:status 200
         :body   (html (fe/render-memo-row word))})

      (and keyup? (some? word) (= key "u"))
      (do
        (db/execute db/conn (remove-word-from-lists-q (:lemma word)))
        (db/execute db/conn (update-book-used-at-q (:book_id word)))
        {:status 200
         :body   (html (fe/render-plain-row word))})

      (and swipe? (= direction "right"))
      {:status 200
       :body   (html (fe/render-op-row word))}

      :else
      {:status 400 :body "error"})))


;; edit book title api


(defn update-book-title-q [book-id title]
  {:update :books
   :set    {:user_entered_title title}
   :where  [:= :id book-id]})


(defn edit-book-title [req]
  (let [book-id (-> req :path-params :book-id)
        title   (-> req :params :title str/trim not-empty)]
    (if-not (empty? title)
      (do
        (db/execute db/conn (update-book-title-q book-id title))
        (println "saving title to db...")
        {:status 200
         :body   (html (fe/book-title {:book-id book-id
                                       :title   title}))}))))



(defn playground [req]
  {:status 200
   :body   (html [:tr.trash [:td {:hx-trigger "touchend"
                                  :hx-get     "/api/playground/"} "okda!"]])})
