(ns legomenon.pages.words-list
  (:require [clojure.string :as str]
            [hiccup.core :refer [html]]

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


(defn render-row [{:keys [keys-allowed list]}
                  {:keys [lemma count id]}]
  (assert keys-allowed)
  (let [hx-trigger (->> keys-allowed
                        (map #(format "key=='%s'" %))
                        (str/join " || ")
                        (format "keyup[%s], swipe[detail.right]"))]
    [:tr
     {:_          "
on touchstart set :x to event.changedTouches[0].screenX
on touchmove set :dx to event.changedTouches[0].screenX - :x then
  if :dx > 40 add .swiping to me end
on touchend remove .swiping from me
"
      :class      (str "dict-word " list)
      :tabindex   "0"
      :hx-trigger hx-trigger
      :hx-post    "/api/words/op/"
      :hx-vals    (format "js:{key: event.key, id: '%s', event: event.type, direction: event.detail.direction}" id)
      :hx-swap    "outerHTML"}
     [:td lemma]
     [:td count]]))


(def render-known-row (partial render-row {:list "known" :keys-allowed ["u"]}))
(def render-trash-row (partial render-row {:list "trash" :keys-allowed ["u"]}))
(def render-memo-row  (partial render-row {:list "memo" :keys-allowed ["u" "k"]}))
(def render-plain-row (partial render-row {:keys-allowed ["k" "t" "m"]}))


(defn render-table-body [words]
  (map (fn [word]
         (case (:list word)
           "known"
           (render-known-row word)

           "trash"
           (render-trash-row word)

           "memo"
           (render-memo-row word)

           (render-plain-row word)))
    words))


(defn words-table [book-id]
  (let [words (db/q db/conn (book-words-q book-id))]
    [:div.row
     [:div.col]
     [:div.col-sm-9
      [:table#swipable
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
