(ns legomenon.pages.words-list
  (:require [hiccup.core :refer [html]]

            [legomenon.db :as db]
            [legomenon.fragments :as fragments]
            [legomenon.book :as book]))


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


(defn render-table-body [words]
  (map (fn [word]
         (case (:list word)
           "known"
           (fragments/render-known-row word)

           "trash"
           (fragments/render-trash-row word)

           "memo"
           (fragments/render-memo-row word)

           "postponed"
           (fragments/render-postponed-row word)

           (fragments/render-plain-row word)))
    words))


(defn words-table [book-id]
  (let [words (db/q db/conn (book-words-q book-id))]
    [:div.row
     [:div.col]
     [:div.col-sm-9
      [:table
       {:_ "
init
  set :dx_to_hightlight to 50
  set :dx_to_be_swiped to 50


on touchstart from <tr/>
  set :x to event.changedTouches[0].screenX
  set :y to event.changedTouches[0].screenY
  set :state to 'none'


on touchmove from <tr/>
  if :state is 'scrolling' exit end

  set :dx to event.changedTouches[0].screenX - :x
  set :dy to event.changedTouches[0].screenY - :y
  set :tr to the closest <tr/> to the event.target

  if :state is 'none' and Math.abs(:dy) > Math.abs(:dx)
    set :state to 'scrolling'
  else
    set :state to 'swiping'
  end

  if :state is 'swiping'
    halt the event
  end

  if :dx > :dx_to_hightlight add .swiping to the :tr
  else remove .swiping from the :tr end


on touchend
  if :state is 'swiping' and :dx > :dx_to_be_swiped
    set word_id to @data-word-id of :tr
    fetch `/fragments/op-row/?word-id=${word_id}`
    then put the result into :tr's outerHTML
    then htmx.process(document.body)
  end

  if :tr exists
   remove .swiping from the :tr then
  end

  set :state to 'none'
"}
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
