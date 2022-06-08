(ns legomenon.fe
  (:require [hiccup.core :refer [html]]
            [hiccup.page :as hiccup.page]
            [legomenon.db :as db]))


(defn page [header & content]
  (hiccup.page/html5 {:encoding "UTF-8"}
    [:head {}
     [:meta {:charset "UTF-8"}]
     [:meta {:name    "viewport"
             :content "width=device-width, initial-scale=1"}]
     (hiccup.page/include-css
       ;; "/public/css/normalize.css"
       "/public/css/bootstrap.min.css"
       "/public/css/main.css"
       )]
    [:body
     (when header
       [:div.header-divider
        header])
     [:div.container
      content]
     (hiccup.page/include-js
       "/public/js/htmx.min.js"
       "/public/js/bootstrap.bundle.min.js")
     [:script
      "
htmx.onLoad(element => {
  if (element.tagName == 'TR' && element.nextSibling.tagName == 'TR') {
    element.nextSibling.focus();
  }});"]]))


(defn navbar [req]
  (let [page     (-> req :reitit.core/match :data :get :page)
        back-btn (case page
                   (:add-book :book-dict) [:a.nav-link {:href "/"} "Go back"]
                   [:a.nav-link "Legomenon"])]
    [:nav.navbar.navbar-dark.navbar-expand-lg
     [:div.container-fluid
      back-btn
      [:button.navbar-toggler {:type           "button"
                               :data-bs-toggle "collapse"
                               :data-bs-target "#navbarNav"
                               :aria-controls  "navbarNav"
                               :aria-expanded  "false"
                               :aria-label     "Toggle navigation"}
       [:span.navbar-toggler-icon]]
      [:div#navbarNav.collapse.navbar-collapse
       [:div.navbar-nav
        [:a.nav-link {:href  "/"
                      :class (when (= page :books-list) "active")} "Books List"]
        [:a.nav-link {:href  "/books/add/"
                      :class (when (= page :add-book) "active")} "Add Book"]]]]]))


;; books list frontend


(defn add-book-form []
  [:form {:method  "post"
          :enctype "multipart/form-data"
          :action  "/api/books/add"}
   ;; [:label {:for "file"} "Choose Book to upload"]
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


(defn index [req]
  {:status 200
   :body   (page
             (navbar req)
             [:div
              [:h4 "Add new book:"]
              (add-book-form)
              [:hr]
              [:h4 "My books:"]
              (books-list)])})


;; book dictionary page frontend


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
  [:tr.dict-word
   {:tabindex   "0"
    :hx-trigger "keyup[key=='k' || key == 't']"
    :hx-post    "/api/words/op/"
    :hx-vals    (format "js:{key: event.key, id: '%s'}" id)
    :hx-swap    "outerHTML"}
   [:td lemma]
   [:td count]])


(defn render-trash-row [{:keys [lemma count id]}]
  [:tr.trash.dict-word
   {:tabindex   "0"
    :hx-trigger "keyup[key == 'u']"
    :hx-post    "/api/words/op/"
    :hx-vals    (format "js:{key: event.key, id: '%s'}" id)
    :hx-swap    "outerHTML"}
   [:td lemma]
   [:td count]])


(defn render-known-row [{:keys [lemma count id]}]
  [:tr.known.dict-word
   {:tabindex   "0"
    :hx-trigger "keyup[key == 'u']"
    :hx-post    "/api/words/op/"
    :hx-vals    (format "js:{key: event.key, id: '%s'}" id)
    :hx-swap    "outerHTML"}
   [:td lemma]
   [:td count]])


(defn words-table [book-id]
  (let [words (db/q db/conn (book-words-q book-id))]
    [:div.row
     [:div.col]
     [:div.col-sm-9
      [:table
       [:thead
        [:tr.dict-word [:th "Word"] [:th "Count"]]]
       [:tbody
        (map (fn [word]
               (cond
                 (pos? (:is_known word))
                 (render-known-row word)

                 (pos? (:is_trash word))
                 (render-trash-row word)

                 :else
                 (render-valuable-row word)))
          words)]]]
     [:div.col]]))


(defn book-title-q [book-id]
  {:from   [:books]
   :select [[[:coalesce :user_entered_title :filename] :title]
            [true :is_book_exists]]
   :where  [:= :id book-id]})


(defn edit-book-title-fragment [req]
  (let [book-id (-> req :params :book-id)
        title   (-> req :params :title)]
    {:status 200
     :body   (html
               [:form {:hx-put (format "/api/books/%s/title/edit/" book-id)}
                [:input.book-title-input {:name "title" :value (or title "")}]
                [:button {:type "submit"} "OK"]])}))


(defn book-title [{:keys [_book-id title]}]
  [:div.row
   [:div.col]
   [:div.col-sm-9
    [:h1 (or title "click to enter title")]]
   [:div.col]])


(defn book-dictionary-page [req]
  (let [book-id                        (-> req :path-params :id)
        {:keys [title is_book_exists]} (db/one db/conn (book-title-q book-id))]
    (if (pos? is_book_exists)
      {:status 200
       :body   (html (page
                       (navbar req)
                       (book-title {:book-id book-id
                                    :title   title})
                       (words-table book-id)))}
      {:status 404
       :body   "not found"})))


;; book text page frontend


(defn book-text [book-id]
  (let [text (:text (db/one db/conn {:from   [:books]
                                     :select [:text]
                                     :where  [:and
                                              [:= :id book-id]
                                              [:= :deleted_at nil]]}))]
    [:div.container.book-text {} text]))


(defn book-text-page [req]
  (let [book-id                        (-> req :path-params :id)
        {:keys [title is_book_exists]} (db/one db/conn (book-title-q book-id))]
    (if (pos? is_book_exists)
      {:status 200
       :body   (html (page nil
                       [:div
                        (book-title {:book-id book-id
                                     :title   title})
                        (book-text book-id)]))}
      {:status 404
       :body   "not found"})))



;; add book page fe


(defn add-book-page [req]
  {:status 200
   :body   "ok"})
