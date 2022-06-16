(ns legomenon.dev
  (:gen-class)
  (:require [clojure.java.io :as io]
            [clojure.tools.namespace.repl :as tn]
            [nrepl.server :as nrepl]
            [cider.nrepl :as cider]
            [mount.core :as mount]
            [nextjournal.beholder :as beholder]

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


(defn reload! []
  (binding [*ns* *ns*]
    (let [res (tn/refresh)]
      (cond
        (= :ok res) :pass

        (instance? Exception res)
        (loop [ex res]
          (when ex
            (println " " (.getMessage ex))
            (recur (.getCause ex))))

        :else (println res)))))


(defn start-autoreload! []
  (beholder/watch (fn [_] (reload!)) "src"))


(defn -main [& _args]
  ;; TODO: move to config?
  (start-nrepl! 8888)
  (start-autoreload!)
  (mount/start))
