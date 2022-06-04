(ns legomenon.fe
  (:require [hiccup.core :refer [html]]
            [hiccup.page :as hiccup.page]
            [legomenon.db :as db]))


(defn page [content]
  (hiccup.page/html5 {:encoding "UTF-8"}
    [:head {}
     [:meta {:charset "UTF-8"}]]
    [:body {}
     content
     (hiccup.page/include-js
       "/public/js/htmx.min.js")]))


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


;; TODO: sort trash and known words at the end
(defn book-words-q [book-id]
  {:from   [:lemma_count]
   :select [:lemma :count :id]
   :where  [:and
            [:> :count 1]
            [:= :book_id book-id]]})


;; TODO: optimize to not be calculated on every request
(defn trash-words []
  (->> (db/q db/conn {:select [:word]
                      :from   [:trash_words]})
       (map :word)
       set))


;; TODO: also optimize
(defn known-words []
  (->> (db/q db/conn {:select [:word]
                      :from   [:known_words]})
       (map :word)
       set))


(defn render-valuable-row [{:keys [lemma count id]}]
  [:tr
   [:td
    [:button {:hx-delete (format "/api/words/%s/mark-as-trash/" id)
              :hx-target "closest tr"
              :hx-swap   "outerHTML"}
     "Trash"]
    [:button {:hx-put    (format "/api/words/%s/mark-as-known/" id)
              :hx-target "closest tr"
              :hx-swap   "outerHTML"}
     "Known"]]
   [:td count]
   [:td lemma]])


(defn render-trash-row [{:keys [lemma count]}]
  [:tr.trash
   [:td "trash"]
   [:td count]
   [:td lemma]])


(defn render-known-row [{:keys [lemma count]}]
  [:tr.known
   [:td "known"]
   [:td count]
   [:td lemma]])


(defn words-table [book-id]
  (let [words       (db/q db/conn (book-words-q book-id))
        trash-words (trash-words)
        known-words (known-words)]
    [:table
     [:tr [:th "Actions"] [:th "Count"] [:th "Word"]]
     (map (fn [word]
            (cond
              (contains? trash-words (:lemma word))
              (render-trash-row word)

              (contains? known-words (:lemma word))
              (render-known-row word)

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

