(ns legomenon.books.views)


(defn add-book-panel []
  ;; NOTE: id also used by `us.views`
  [:div#uploading-status
   [:h4 "Add new book:"]
   [:form {:hx-post     "/api/books/add"
           :hx-encoding "multipart/form-data"
           :hx-swap     "outerHTML"
           :hx-target   "#uploading-status"}
    [:input {:type "file" :id "file" :name "file"}]
    [:button "Submit"]]])


(defn render-book [{:keys [title id]}]
  [:div {}
   [:a {:href (format "/books/%s/" id)} title]
   " ("
   [:a {:href (format "/books/%s/text/" id)} "text"] ", "
   [:a {:href (format "/books/%s/settings/" id)} "settings"]
   ")"])
