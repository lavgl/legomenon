(ns legomenon.api.words-operations
  (:require  [hiccup.core :refer [html]]

             [legomenon.db :as db]
             [legomenon.fragments :as fragments]))



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


(defn handler [req]
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
         :body   (html (fragments/render-known-row word))})

      (and keyup? (some? word) (= key "t"))
      (do
        (db/execute db/conn (add-word-to-list-q :trash (:lemma word)))
        (db/execute db/conn (update-book-used-at-q (:book_id word)))
        {:status 200
         :body   (html (fragments/render-trash-row word))})

      (and keyup? (some? word) (= key "m"))
      (do
        (db/execute db/conn (add-word-to-list-q :memo (:lemma word)))
        (db/execute db/conn (update-book-used-at-q (:book_id word)))
        {:status 200
         :body   (html (fragments/render-memo-row word))})

      (and keyup? (some? word) (= key "u"))
      (do
        (db/execute db/conn (remove-word-from-lists-q (:lemma word)))
        (db/execute db/conn (update-book-used-at-q (:book_id word)))
        {:status 200
         :body   (html (fragments/render-plain-row word))})

      (and keyup? (some? word) (= key "p"))
      (do
        (db/execute db/conn (add-word-to-list-q :postponed (:lemma word)))
        (db/execute db/conn (update-book-used-at-q (:book_id word)))
        {:status 200
         :body   (html (fragments/render-postponed-row word))})

      (and swipe? (= direction "right"))
      {:status 200
       :body   (html (fragments/render-op-row word))}

      :else
      {:status 400 :body "error"})))


(defn render-op-row [req]
  (let [word-id (-> req :params :word-id)
        word    (db/one db/conn (word-q word-id))]
    {:status 200
     :body   (html (fragments/render-op-row word))}))
