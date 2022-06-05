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
  {:select [:id
            [[:coalesce :user_entered_title :filename] :title]]
   :from   [:books]
   :where  [:= nil :deleted_at]})


(defn render-book [{:keys [title id]}]
  [:div {}
   [:a {:href (format "/books/%s/" id)} title]])


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
     :left-join [[:trash_words :trash] [:and
                                        [:= :trash.word :lemma_count.lemma]
                                        [:= :trash.deleted_at nil]]
                 [:known_words :known] [:and
                                        [:= :known.word :lemma_count.lemma]
                                        [:= :known.deleted_at nil]]]
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
        :hx-post    "/api/words/op/"
        :hx-vals    (format "js:{key: event.key, id: '%s'}" id)
        :hx-swap    "outerHTML"}
   [:td lemma]
   [:td count]])


(defn render-trash-row [{:keys [lemma count id]}]
  [:tr.trash {:tabindex   "0"
              :hx-trigger "keyup[key == 'u']"
              :hx-post    "/api/words/op/"
              :hx-vals    (format "js:{key: event.key, id: '%s'}" id)
              :hx-swap    "outerHTML"}
   [:td lemma]
   [:td count]])


(defn render-known-row [{:keys [lemma count id]}]
  [:tr.known {:tabindex   "0"
              :hx-trigger "keyup[key == 'u']"
              :hx-post    "/api/words/op/"
              :hx-vals    (format "js:{key: event.key, id: '%s'}" id)
              :hx-swap    "outerHTML"}
   [:td lemma]
   [:td count]])


(defn words-table [book-id]
  (let [words (db/q db/conn (book-words-q book-id))]
    [:table
     [:tr [:th "Word"] [:th "Count"]]
     (map (fn [word]
            (cond
              (pos? (:is_known word))
              (render-known-row word)

              (pos? (:is_trash word))
              (render-trash-row word)

              :else
              (render-valuable-row word)))
       words)]))


(defn book-title-q [book-id]
  {:from   [:books]
   :select [[:user_entered_title :title]
            [true :is_book_exists]]
   :where  [:= :id book-id]})


(defn edit-book-title-fragment [req]
  (let [book-id (-> req :params :book-id)
        title   (-> req :params :title)]
    {:status 200
     :body   (html
               [:form {:hx-put (format "/api/books/%s/title/edit/" book-id)}
                [:input.book-title-input {:name "title" :value title}]
                [:button {:type "submit"} "OK"]])}))


(defn book-title [{:keys [book-id title]}]
  [:h1 {:hx-get  (format "/fragments/edit-book-title/?book-id=%s&title=%s" book-id title)
        :hx-swap "outerHTML"}
   (or title "click to enter title")])


(defn book-page [req]
  (let [book-id                        (-> req :path-params :id)
        {:keys [title is_book_exists]} (db/one db/conn (book-title-q book-id))]
    (if (pos? is_book_exists)
      {:status 200
       :body   (html (page
                       [:div
                        (book-title {:book-id book-id
                                     :title   title})
                        (words-table book-id)]))}
      {:status 404
       :body   "not found"})))

