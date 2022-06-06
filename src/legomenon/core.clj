(ns legomenon.core
  (:require [clojure.tools.logging :as log]
            [mount.core :as mount]
            [aleph.http :as http]
            [reitit.ring :as ring]
            [ring.logger :refer [wrap-with-logger]]
            [ring.util.response :as response]
            [ring.middleware.reload :refer [wrap-reload]]
            [ring.middleware.params :refer [wrap-params]]
            [ring.middleware.keyword-params :refer [wrap-keyword-params]]
            [ring.middleware.multipart-params :refer [wrap-multipart-params]]

            [legomenon.fe :as fe]
            [legomenon.api :as api]))

;; TODO: move to config
(def PORT 5000)


(defn make-app []
  (let [routes [["/" {:get {:handler fe/index}}]
                ["/books/:id/" {:get {:handler fe/book-dictionary-page}}]
                ["/books/:id/text/" {:get {:handler fe/book-text-page}}]

                ["/fragments/edit-book-title/" {:get {:handler fe/edit-book-title-fragment}}]

                ["/api/books/add/" {:post {:handler api/add-book}}]
                ["/api/books/:book-id/title/edit/" {:put {:handler api/edit-book-title}}]
                ["/api/words/op/" {:post {:handler api/operate-on-word}}]

                ["/public/*" (ring/create-resource-handler)]]
        default (ring/routes
                  (ring/redirect-trailing-slash-handler {:method :add})
                  (ring/create-default-handler))]
    (-> (ring/router routes)
        (ring/ring-handler default)
        (wrap-keyword-params)
        (wrap-params)
        (wrap-multipart-params)
        (wrap-with-logger)
        (wrap-reload))))


(mount/defstate server
  :start (let [server (http/start-server (make-app) {:port PORT})]
           (log/info "Started server on" PORT)
           server)
  :stop (do
          (log/info "Stopping server on" PORT)
          (.close server)))


(defn -main [& _args]
  (mount/start #'server))


;; development stuff

(defn start []
  (mount/start))


(defn stop []
  (mount/stop))


(defn reload []
  (stop)
  (start))


