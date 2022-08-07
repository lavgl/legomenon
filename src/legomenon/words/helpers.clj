(ns legomenon.words.helpers)


(defn occur-rate [word-count total-words-count]
  (format "%.2f"
    (-> word-count
        (/ total-words-count)
        (* 1e4))))


(defn assign-occur-rate-to-first-by-group [total-words-count words]
  (->> words
       (partition 2 1)
       (map (fn [[a b]]
              (cond-> b
                (not= (:count a) (:count b))
                (assoc :occur-rate (occur-rate (:count b) total-words-count)))))))
