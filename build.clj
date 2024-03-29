(ns build
  (:require [clojure.tools.build.api :as b]
            ;; [clojure.tools.build.tasks.copy :as copy]
            ))


(def class-dir "target/classes")
(def basis (b/create-basis {:project "deps.edn"}))
(def sha (b/git-process {:git-args "rev-parse --short HEAD"}))
(def uber-file (format "target/legomenon-%s.jar" sha))


(defn clean [_]
  (b/delete {:path "target"}))


(defn uber [_]
  (clean nil)
  (b/copy-dir {:src-dirs   ["src" "resources"]
               ;; :ignores    (conj copy/default-ignores "dev.clj")
               :target-dir class-dir})
  (b/compile-clj {:basis      basis
                  ;; :src-dirs  ["src"]
                  :ns-compile ['legomenon.core]
                  :class-dir  class-dir})
  (b/uber {:class-dir class-dir
           :uber-file uber-file
           :exclude   ["LICENSE"]
           :basis     basis
           :main      'legomenon.core}))
