(ns legomenon.words.dal
  (:require [legomenon.db :as db]
            [legomenon.top-words :as top-words]))


(def words-sorting-subq
  [:case
   [:= :list "postponed"] 4
   [:= :list nil]         3
   [:= :list "memo"]      2
   [:= :list "known"]     1
   [:= :list "trash"]     0])


(defn book-words-q [book-id]
  {:from      [:lemma_count]
   :left-join [:my_words [:and
                          [:= :my_words.word :lemma_count.lemma]
                          [:= :my_words.deleted_at nil]]]
   :select    [:lemma :count :id :my_words.list]
   :where     [:and
               [:= :lemma_count.book_id book-id]
               [:> :lemma_count.count 1]]
   :order-by  [[words-sorting-subq :desc]
               [:count :desc]]})


(defn words-list-by-book-id [book-id show]
  (cond->> (db/q (book-words-q book-id))
    (= show "top-4k")  (filter #(top-words/in-4k? (:lemma %)))
    (= show "top-10k") (filter #(top-words/in-10k? (:lemma %)))))
