(ns legomenon.fe
  (:require [hiccup.core :refer [html]]
            [legomenon.db :as db]))


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
   :body   (html [:div
                  [:h1 "Hey, this is the header"]
                  (add-book-form)
                  (books-list)])})


;; book page frontend


(defn book-q [book-id]
  {:from   [:lemma_count]
   :select [:lemma :count]
   :where  [:= :book_id book-id]})


(defn lemmas-table [book-id]
  (let [lemmas (db/q db/conn (book-q book-id))]
    [:table
     [:tr [:th "Lemma"] [:th "Count"]]
     (map (fn [{:keys [lemma count]}]
             [:tr {:tabindex "0"} [:td lemma] [:td count]]) lemmas)]))



(defn book-exists? [book-id]
  (let [q {:select [1]
           :where  [:exists {:select [1]
                             :from   [:books]
                             :where  [:= :id book-id]}]}]
    (->> (db/q db/conn q)
         not-empty
         boolean)))


(defn book-page [req]
  (let [book-id (-> req :path-params :id)]
    (if (book-exists? book-id)
      {:status 200
       :body   (html [:div
                      [:h1 "book title could be here"]
                      (lemmas-table book-id)])}
      {:status 404
       :body   "not found"})))

