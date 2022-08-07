(ns legomenon.book)


(defn title-q [book-id]
  {:from   [:books]
   :select [[[:coalesce :user_entered_title :filename] :title]
            [true :is_book_exists]]
   :where  [:= :id book-id]})


(defn render-title [{:keys [title]}]
  [:div.row
   [:div.col]
   [:div.col-sm-9
    [:h1 title]]
   [:div.col]])
