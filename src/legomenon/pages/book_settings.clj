(ns legomenon.pages.book-settings
  (:require [clojure.string :as str]

            [legomenon.db :as db]
            [legomenon.fragments :as fragments]
            [legomenon.book :as book]))


(defn rename-block [book-id title]
  [:form {:hx-put    (format "/api/books/%s/rename/" book-id)
          :hx-params "*"}
   [:label {:for "new-book-name"} "New book name"]
   [:div.mb-3.input-group
    [:input#new-book-name.form-control {:value title
                                        :name  "title"}]
    [:button.btn.btn-info.btn-lg.settings-btn {:type "submit"}
     "Rename"]]])


(defn delete-block [book-id]
  [:div.mb-3
   [:button.btn.btn-danger.btn-lg.settings-btn
    {:hx-confirm "Are you sure?"
     :hx-delete  (format "/api/books/%s/" book-id)}
    "Delete"]])


(defn page [req]
  (let [book-id                        (-> req :path-params :id)
        {:keys [title is_book_exists]} (db/one db/conn (book/title-q book-id))]
    (if (pos? is_book_exists)
      {:status 200
       :body   (fragments/page
                 (fragments/navbar req)
                 [:div
                  [:h1 (format "%s settings" title)]
                  (rename-block book-id title)
                  (delete-block book-id)])}
      {:status 404
       :body   "not found"})))


(defn update-book-title-q [book-id new-title]
  {:update :books
   :set    {:user_entered_title new-title}
   :where  [:= :id book-id]})


(defn rename-book-handler [req]
  (let [new-title                (-> req :params :title str/trim not-empty)
        book-id                  (-> req :path-params :id)
        {:keys [is_book_exists]} (db/one db/conn (book/title-q book-id))]
    (if (and is_book_exists new-title)
      (do
        (db/execute db/conn (update-book-title-q book-id new-title))
        {:status  200
         :headers {"HX-Refresh" true}})
      {:status 400
       :body   "bad request"})))


(defn delete-book-q [book-id]
  {:delete-from :books
   :where       [:= :id book-id]})


(defn delete-book-words-q [book-id]
  {:delete-from :lemma_count
   :where       [:= :book_id book-id]})


(defn delete-book-db! [book-id]
  (db/with-tx [tx db/conn]
    (db/execute tx (delete-book-words-q book-id))
    (db/execute tx (delete-book-q book-id))))


(defn delete-book-handler [req]
  (let [book-id                  (-> req :path-params :id)
        {:keys [is_book_exists]} (db/one db/conn (book/title-q book-id))]
    (if is_book_exists
      (do
        (delete-book-db! book-id)
        {:status  200
         ;; TODO: user-friendly message here?
         :headers {"HX-Redirect" "/?message=book-successfully-deleted"}})
      {:status 404
       :body   "not found"})))
