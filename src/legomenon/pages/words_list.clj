(ns legomenon.pages.words-list
  (:require [hiccup.core :refer [html]]

            [legomenon.db :as db]
            [legomenon.fragments :as fragments]
            [legomenon.book :as book]))


(defn book-words-q [book-id]
  (let [priority [:case
                  [:= :list "memo"]  3
                  [:= :list nil]     2
                  [:= :list "known"] 1
                  [:= :list "trash"] 0]]
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


(defn render-table-body [words]
  (map (fn [word]
         (case (:list word)
           "known"
           (fragments/render-known-row word)

           "trash"
           (fragments/render-trash-row word)

           "memo"
           (fragments/render-memo-row word)

           (fragments/render-plain-row word)))
    words))


(defn words-table [book-id]
  (let [words (db/q db/conn (book-words-q book-id))]
    [:div.row
     [:div.col]
     [:div.col-sm-9
      [:table#swipable
       {:_ "
on touchstart from <tr/> set :x to event.changedTouches[0].screenX
on touchmove from <tr/> set :dx to event.changedTouches[0].screenX - :x then
if :dx > 30 add .swiping to the closest <tr/> to the event.target end
on touchend remove .swiping from the closest <tr/> to the event.target"}
       [:thead
        [:tr.dict-word [:th "Word"] [:th "Count"]]]
       [:tbody (render-table-body words) ]]]
     [:div.col]]))


(defn page [req]
  (let [book-id                        (-> req :path-params :id)
        {:keys [title is_book_exists]} (db/one db/conn (book/title-q book-id))]
    (if (pos? is_book_exists)
      {:status 200
       :body   (html (fragments/page
                       (fragments/navbar req)
                       (book/render-title {:book-id book-id
                                           :title   title})
                       (words-table book-id)))}
      {:status 404
       :body   "not found"})))
