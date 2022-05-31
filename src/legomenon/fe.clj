(ns legomenon.fe
  (:require [hiccup.core :refer [html]]))


(defn index [& _]
  {:status 200
   :body   (html [:div
                [:h1 "Hey, this is the header"]
                [:p "hey! and this is the paragraph"]])})

