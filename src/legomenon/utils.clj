(ns legomenon.utils
  (:import [java.security MessageDigest]
           [java.util UUID])
  (:require [clojure.string :as str]))


;; ================================================


(defn md5 [^String s]
  (let [algorithm (MessageDigest/getInstance "MD5")
        raw       (.digest algorithm (.getBytes s))]
    (format "%032x" (BigInteger. 1 raw))))


(defn uuid []
  (java.util.UUID/randomUUID))


;; ================================================
;; classname util
;; credits to https://github.com/ajchemist/classname/blob/master/src/classname/core.cljc
;; ================================================


(defn- parse-args [xs]
  (loop [res {}, xs xs]
    (let [x (first xs), rest (rest xs)]
      (cond
        (string?     x) (recur (assoc res x        true) rest)
        (keyword?    x) (recur (assoc res (name x) true) rest)
        (symbol?     x) (recur (assoc res (name x) true) rest)
        (number?     x) (recur (assoc res (str x)  true) rest)
        (map?        x) (recur (reduce #(assoc %1 (name (key %2)) (val %2)) res x) rest)
        (sequential? x) (recur (merge res (parse-args x)) rest)
        (set?        x) (recur (merge res (parse-args (seq x))) rest)
        :else           (if (empty? rest) res (recur res rest))))))


(defn classname
  "Merge-like classname utility"
  [& xs]
  (->> xs
       (parse-args)
       (filter #(val %))
       (keys)
       (str/join " ")))
