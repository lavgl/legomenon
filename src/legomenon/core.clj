(ns legomenon.core
  (:require [clojure.tools.logging :as log]
            [mount.core :as mount]
            [aleph.http :as http]
            [reitit.ring :as ring]
            [ring.logger :refer [wrap-with-logger]]
            [ring.util.response :as response]
            [ring.middleware.reload :refer [wrap-reload]]
            [ring.middleware.keyword-params :refer [wrap-keyword-params]]
            [ring.middleware.multipart-params :refer [wrap-multipart-params]]

            [legomenon.fe :as fe]
            [legomenon.api :as api]))

;; TODO: move to config
(def PORT 5000)


(defn make-app []
  (let [routes [["/" {:get {:handler fe/index}}]
                ["/books/:id/" {:get {:handler fe/book-page}}]

                ["/api/books/add/" {:post {:handler api/add-book}}]
                ["/api/words/:id/mark-as-trash/" {:delete {:handler api/mark-word-as-trash}}]
                ["/api/words/:id/mark-as-known/" {:put {:handler api/mark-word-as-known}}]

                ["/public/*" (ring/create-resource-handler)]]
        default (ring/routes
                  (ring/redirect-trailing-slash-handler {:method :add})
                  (ring/create-default-handler))]
    (-> (ring/router routes)
        (ring/ring-handler default)
        (wrap-keyword-params)
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


