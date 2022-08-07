(ns legomenon.words.dal
  (:require [legomenon.db :as db]))


(defn book-words-q [book-id]
  (let [priority [:case
                  [:= :list "memo"]      4
                  [:= :list nil]         3
                  [:= :list "postponed"] 2
                  [:= :list "known"]     1
                  [:= :list "trash"]     0]]
    {:from      [:lemma_count]
     :left-join [:my_words [:and
                            [:= :my_words.word :lemma_count.lemma]
                            [:= :my_words.deleted_at nil]]]
     :select    [:lemma :count :id :my_words.list]
     :where     [:and
                 [:= :lemma_count.book_id book-id]
                 [:> :lemma_count.count 1]]
     :order-by  [[priority :desc]
                 [:count :desc]]}))


(defn words-list-by-book-id [book-id]
  (db/q db/conn (book-words-q book-id)))
