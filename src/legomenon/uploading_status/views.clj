(ns legomenon.uploading-status.views
  (:require [legomenon.uploading-status.vars :as us.vars]))


(defn with-checking-status [id & body]
  ;; NOTE: id also used by `pages.book-list`
  [:div#uploading-status
   {:hx-target  "#uploading-status"
    :hx-get     (format "/api/uploading/%s/status/" id)
    :hx-trigger "load delay:1s"
    :hx-swap    "outerHTML"}
   body])


(defn progress-bar [current]
  [:div.progress {:style "height: 20px;"}
   [:div#pb.progress-bar.progress-bar-striped.progress-bar-animated
    {:aria-valuenow current
     :aria-valuemin "0"
     :aria-valuemax "100"
     :style         (format "width: %s%%;" current)}]])


(defn uploading-status-initialized [{:keys [filename]}]
  [:div
   [:h3 (format "Uploading %s" filename)]
   (progress-bar 100)])


(defn uploading-status-in-progress [{:keys [filename state_info]}]
  [:div
   [:h3 (format "Uploading %s" filename)]
   (let [current-percent (:current_percent state_info)]
     (progress-bar current-percent))])


(defn uploading-status-error [{:keys [filename]}]
  [:h3 (format "Error while uploading %s :(" filename)])


(defn uploading-status [id {:keys [state state_info filename]}]
  (condp = state
    us.vars/STATE-STEP-1
    (with-checking-status id
      (uploading-status-initialized {:filename filename}))

    us.vars/STATE-STEP-2
    (with-checking-status id
      (uploading-status-in-progress {:filename   filename
                                     :state_info state_info}))

    us.vars/STATE-ERROR
    (uploading-status-error {:filename filename})))
