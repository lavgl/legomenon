(ns legomenon.uploading-status.api
  (:require [hiccup.core :refer [html]]

            [legomenon.uploading-status.vars :as us.vars]
            [legomenon.uploading-status.views :as us.views]
            [legomenon.uploading-status.dal :as us.dal]))


(defn handler [req]
  (let [id     (-> req :path-params :id)
        status (us.dal/get-by-id id)]
    (if (= us.vars/STATE-DONE (:state status))
      {:status  301
       :headers {"HX-Redirect" "/"}}
      {:status 200
       :body   (html (us.views/uploading-status id status))})))
