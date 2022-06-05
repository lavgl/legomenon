(ns legomenon.fe
  (:require [hiccup.core :refer [html]]
            [hiccup.page :as hiccup.page]
            [legomenon.db :as db]))


(defn page [content]
  (hiccup.page/html5 {:encoding "UTF-8"}
    [:head {}
     [:meta {:charset "UTF-8"}]
     (hiccup.page/include-css "/public/css/main.css")]
    [:body
     [:div.content
      content]
     (hiccup.page/include-js "/public/js/htmx.min.js")
     [:script
      "
htmx.onLoad(element => {
  if (element.tagName == 'TR' && element.nextSibling.tagName == 'TR') {
    element.nextSibling.focus();
  }});"]]))


;; books list frontend


(defn add-book-form []
  [:form {:method  "post"
          :enctype "multipart/form-data"
          :action  "/api/books/add"}
   [:label {:for "file"} "Choose Book to upload"]
   [:input {:type "file" :id "file" :name "file"}]
   [:button "Submit"]])


(defn books-q []
  {:select [:filename :id]
   :from   [:books]
   :where  [:= nil :deleted_at]})


(defn render-book [{:keys [filename id]}]
  [:div {}
   [:a {:href (format "/books/%s/" id)} filename]])


(defn books-list []
  (let [books (db/q db/conn (books-q))]
    [:div {} (map render-book books)]))


(defn index [& _]
  {:status 200
   :body   (page [:div
                  [:h1 "Hey, this is the header"]
                  (add-book-form)
                  (books-list)])})


;; book page frontend



(defn book-words-q [book-id]
  (let [is-trash-subq [:case
                       [:!= :trash.word nil] 1
                       :else 0]
        is-known-subq [:case
                       [:!= :known.word nil] 1
                       :else 0]]
    {:from      [:lemma_count]
     :left-join [[:trash_words :trash] [:= :trash.word :lemma_count.lemma]
                 [:known_words :known] [:= :known.word :lemma_count.lemma]]
     :select    [:lemma :count :id
                 [is-trash-subq :is_trash]
                 [is-known-subq :is_known]]
     :where     [:and
                 [:= :lemma_count.book_id book-id]
                 [:> :lemma_count.count 1]]
     :order-by  [[:is_trash :asc]
                 [:is_known :asc]
                 [:count :desc]]}))


(defn render-valuable-row [{:keys [lemma count id]}]
  [:tr {:tabindex   "0"
        :hx-trigger "keyup[key=='k' || key == 't']"
        :hx-post    "/api/words/mark/"
        :hx-vals    (format "js:{key: event.key, id: '%s'}" id)
        :hx-swap    "outerHTML"}
   [:td]
   [:td lemma]
   [:td count]])


(defn render-trash-row [{:keys [lemma count]}]
  [:tr.trash
   [:td]
   [:td lemma]
   [:td count]])


(defn render-known-row [{:keys [lemma count]}]
  [:tr.known
   [:td]
   [:td lemma]
   [:td count]])


(defn words-table [book-id]
  (let [words (db/q db/conn (book-words-q book-id))]
    [:table
     [:tr [:th "Actions"] [:th "Word"] [:th "Count"]]
     (map (fn [word]
            (cond
              (pos? (:is_known word))
              (render-known-row word)

              (pos? (:is_trash word))
              (render-trash-row word)

              :else
              (render-valuable-row word)))
       words)]))


(defn book-exists? [book-id]
  (db/exists? {:select [1]
               :from   [:books]
               :where  [:= :id book-id]}))


(defn book-page [req]
  (let [book-id (-> req :path-params :id)]
    (if (book-exists? book-id)
      {:status 200
       :body   (html (page
                       [:div
                        [:h1 "book title could be here"]
                        (words-table book-id)]))}
      {:status 404
       :body   "not found"})))

