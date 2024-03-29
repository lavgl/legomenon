(ns legomenon.core
  (:gen-class)
  (:require [clojure.tools.logging :as log]
            [mount.core :as mount]
            [aleph.http :as http]
            [reitit.ring :as ring]
            [reitit.exception :as reitit.exception]
            [ring.logger :refer [wrap-with-logger]]
            [ring.middleware.params :refer [wrap-params]]
            [ring.middleware.keyword-params :refer [wrap-keyword-params]]
            [ring.middleware.multipart-params :refer [wrap-multipart-params]]

            [legomenon.api.add-book :as add-book]
            [legomenon.api.words-operations :as words-op]
            [legomenon.books-aggregations.api :as aggs.api]
            [legomenon.uploading-status.api :as us.api]
            [legomenon.pages.words-list :as words-list]
            [legomenon.pages.book-text :as book-text]
            [legomenon.pages.book-settings :as book-settings]
            [legomenon.pages.main-page :as main]
            [legomenon.pages.new-book-aggregation :as new-agg]
            [legomenon.pages.book-aggregation :as agg]

            [legomenon.config]
            [legomenon.db]))


;; TODO: move to config
(def PORT 5000)


(defn log-conflicts [conflicts]
  (println (reitit.exception/format-exception :path-conflicts nil conflicts)))


(defn make-app []
  ;; NOTE: :page is used for navbar building
  (let [routes [["/" {:get  {:handler main/page}
                      :page :books-list}]
                ["/books/:id/" {:get {:handler words-list/page
                                      :page    :book-dict}}]
                ["/books/:id/text/" {:get {:handler book-text/page
                                           :page    :book-text}}]
                ["/books/:id/settings/" {:get {:handler book-settings/page
                                               :page    :book-settings}}]
                ["/aggs/new/" {:handler new-agg/page
                               :page    :new-agg}]
                ["/aggs/:id/" {:handler agg/page
                               :page    :agg}]

                ["/fragments/op-row/" {:get {:handler words-op/render-op-row}}]

                ["/api/books/add/" {:post {:handler add-book/handler}}]
                ["/api/books/:id/" {:delete {:handler book-settings/delete-book-handler}}]
                ["/api/books/:id/rename/" {:put {:handler book-settings/rename-book-handler}}]
                ["/api/words/op/" {:post {:handler words-op/handler}}]
                ["/api/uploading/:id/status/" {:get {:handler us.api/handler}}]
                ["/api/aggs/add/" {:post {:handler aggs.api/handler}}]

                ["/public/*" (ring/create-resource-handler)]]
        default (ring/routes
                  (ring/redirect-trailing-slash-handler {:method :add})
                  (ring/create-default-handler))]
    (-> (ring/router routes {:conflicts log-conflicts})
        (ring/ring-handler default)
        (wrap-keyword-params)
        (wrap-params)
        (wrap-multipart-params)
        (wrap-with-logger))))


(mount/defstate server
  :start (let [server (http/start-server (make-app) {:port PORT})]
           (log/info "Started server on" PORT)
           server)
  :stop (do
          (log/info "Stopping server on" PORT)
          (.close server)))


(defn -main [& _args]
  (mount/start))
