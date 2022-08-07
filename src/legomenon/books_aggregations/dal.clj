(ns legomenon.books-aggregations.dal
  (:require [legomenon.db :as db]
            [legomenon.utils :as utils]))


(defn agg-by-id-q [id]
  {:from   [:books_aggregations]
   :select [:*]
   :where  [:= :id id]})


(defn agg-by-id [id]
  (db/one db/conn (agg-by-id-q id)))


(defn aggs-list-q []
  {:from   [:books_aggregations]
   :select [:id :name]
   :where  [:= nil :deleted_at]})


(defn aggs-list []
  (db/q db/conn (aggs-list-q)))


(defn insert-agg-q [agg-name]
  {:insert-into :books_aggregations
   :values      [{:id   (utils/uuid)
                  :name agg-name}]
   :returning   [:id]})


(defn aggs-words-q [books-id]
  {:from      [:lemma_count]
   :left-join [:my_words [:and
                          [:= :my_words.word :lemma_count.lemma]
                          [:= :my_words.deleted_at nil]]]
   :select    [:lemma_count.lemma
               [[:sum :lemma_count.count] :count]]
   :where     [:in :lemma_count.book_id books-id]
   :group-by  [:lemma_count.lemma]
   :having    [:> [:sum :lemma_count.count] 1]
   :order-by  [[:count :desc]]})


;; TODO: extract in dal and reuse on book creation?
(defn words->db [words agg-id]
  (letfn [(map-fn [{:keys [lemma count]}]
            {:lemma   lemma
             :count   count
             :book_id agg-id})]
    (map map-fn words)))


;; TODO: extract in dal and reuse on book creation?
(defn insert-words-q [values]
  {:insert-into :lemma_count
   :values      values})


(defn create-agg [agg-name books-ids]
  (db/with-tx [tx db/conn]
    (let [words        (db/q db/conn (aggs-words-q books-ids))
          {:keys [id]} (db/one tx (insert-agg-q agg-name))
          words-db     (words->db words id)]
      (db/execute tx (insert-words-q words-db)))))
