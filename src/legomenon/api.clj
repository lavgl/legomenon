(ns legomenon.api
  (:require [clojure.java.io :as io]
            [dk.simongray.datalinguist :as nlp]
            [pantomime.extract :as extract]

            [legomenon.db :as db]
            [legomenon.utils :as utils]))


(defn insert-book-q [book]
  {:insert-into [:books]
   :values      [book]})


(defn book->db [{:keys [filename text]}]
  {:filename  filename
   :text      text
   :text_hash (utils/sha256 text)})


(defn parse-book [{:keys [filename tempfile]}]
  (let [text (->> (io/input-stream tempfile)
                  extract/parse
                  :text)]
    {:filename filename
     :text     text}))


(defn add-book [req]
  (let [file (-> req :params :file)
        book (parse-book file)]
    (try
      (db/execute db/conn (insert-book-q (book->db book)))
      {:status  301
       :headers {"Location" "/"}}
      (catch Exception e
        {:status 301
         :headers {"Location" "/?message=book-duplicate"}}))))
