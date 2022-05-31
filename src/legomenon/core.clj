(ns legomenon.core
  (:require [clojure.tools.logging :as log]
            [mount.core :as mount]
            [aleph.http :as http]
            [reitit.ring :as ring]
            [ring.logger :as logger]
            [ring.middleware.reload :as ring-devel]

            [legomenon.fe :as fe]))

;; TODO: move to config
(def PORT 5000)


(defn handler [_req]
  {:status 200
   :body   "Hello World"})


(defn bye-handler [_req]
  {:status 200
   :body "Bye"})


(defn not-found [_]
  {:status 404
   :body "Not found"})


(defn make-app []
  (let [routes [["/" {:get {:handler fe/index}}]]]
    (-> (ring/router routes)
        (ring/ring-handler not-found)
        (logger/wrap-with-logger)
        (ring-devel/wrap-reload))))


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


