(ns legomenon.dev
  (:gen-class)
  (:require [clojure.java.io :as io]

            [nrepl.server :as nrepl]
            [cider.nrepl :as cider]
            [mount.core :as mount]
            [legomenon.core]))


(defn start-nrepl! [port]
  (doto (io/file ".nrepl-port")
    (spit port)
    (.deleteOnExit))
  (nrepl/start-server
    :port port
    :bind "127.0.0.1"
    :handler (->> (map resolve cider/cider-middleware)
                  (apply nrepl/default-handler))))


(defn -main [& _args]
  ;; TODO: move to config?
  (start-nrepl! 8888)
  (mount/start))
