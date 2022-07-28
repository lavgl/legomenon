(ns legomenon.api.add-book
  (:refer-clojure :exclude [number?])
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [hiccup.core :refer [html]]
            [mount.core :as mount]
            [pantomime.extract :as extract]
            [dk.simongray.datalinguist :as nlp]
            [better-cond.core :as b]
            [tick.core :as t]

            [legomenon.db :as db]
            [legomenon.utils :as utils]
            [legomenon.fragments :as fragments]))


(defn insert-book-q [book]
  {:insert-into [:books]
   :values      [book]})


(defn book-q [id]
  {:select [:id]
   :from   [:books]
   :where  [:= :id id]})


(defn init-progress-q []
  {:insert-into [:uploading_progress]
   :values      [{:current_percent 0}]
   :returning   [:id]})


(defn update-progress-q [id progress]
  {:update :uploading_progress
   :set    {:current_percent progress}
   :where  [:= :id id]})


(defn cleanup-progress-q [id]
  {:delete-from :uploading_progress
   :where       [:= :id id]})


(defn book->db [{:keys [filename text]}]
  {:id       (utils/md5 text)
   :filename filename
   :text     text})


(defn parse-book [{:keys [filename tempfile]}]
  (let [text (->> (io/input-stream tempfile)
                  extract/parse
                  :text)]
    {:filename filename
     :text     text}))


;; lemmas extraction


(mount/defstate sentence-splitter
  :start (nlp/->pipeline {:annotators ["ssplit"]}))


(mount/defstate nlp
  :start (nlp/->pipeline {:annotators ["lemma"]}))


(mount/defstate words
  :start (->> (slurp (io/resource "words"))
              (str/split-lines)
              set))


(def ^:dynamic *on-sentence-processed-cb* nil)


(defmacro with-on-sentence-processed-cb [cb & body]
  `(binding [*on-sentence-processed-cb* ~cb]
     ~@body))


(defn word-exists? [s]
  (contains? words s))


(defn remove-explicit-line-breaks [text]
  (str/replace text "-\n" ""))


(defn split-sentences [text]
  (->> text
       sentence-splitter
       nlp/sentences
       nlp/text))


(defonce progress-timer (atom {}))

(defn save-progress! [{:keys [progress-id current total]}]
  (let [last-write (get @progress-timer progress-id)
        second-ago (t/<< (t/now) (t/of-seconds 1))]
    (when (or (nil? last-write)
              (t/< last-write second-ago))
      (let [percent (int (Math/floor (* 100 (/ current total))))]
        (db/execute db/conn (update-progress-q progress-id percent))
        (swap! progress-timer assoc progress-id (t/now))))))


(defn cleanup-progress [progress-id]
  (swap! progress-timer dissoc progress-id)
  (db/execute db/conn (cleanup-progress-q progress-id)))


;; TODO: remove progress-id from args?
(defn lemma-frequencies [{:keys [text progress-id]}]
  (let [sentenses (->> (remove-explicit-line-breaks text)
                       (split-sentences))
        xf        (comp
                    (map-indexed (fn [i s]
                                   (when *on-sentence-processed-cb*
                                     (*on-sentence-processed-cb*
                                       {:progress-id progress-id
                                        :current     i
                                        :total       (count sentenses)}))
                                   s))
                    (map nlp)
                    (mapcat nlp/tokens)
                    (map nlp/lemma)
                    (filter word-exists?)
                    (map str/lower-case))]
    (->> sentenses
         (transduce xf conj)
         frequencies
         (sort-by second >))))


(defn insert-lemmas-q [values]
  {:insert-into [:lemma_count]
   :values      values})


(defn lemmas->db [lemmas-count book-id]
  (letfn [(map-fn [[lemma lemma-count]]
            {:lemma   lemma
             :count   lemma-count
             :book_id book-id})]
    (map map-fn lemmas-count)))


;; add book api


(defn process-book! [progress-id book]
  (db/execute db/conn (insert-book-q book))

  (let [lemmas    (lemma-frequencies {:progress-id progress-id
                                      :text        (:text book)})
        lemmas-db (lemmas->db lemmas (:id book))]
    (db/execute db/conn (insert-lemmas-q lemmas-db))
    (cleanup-progress progress-id)))


(defn handler [req]
  (b/cond
    :let [file    (-> req :params :file)
          book    (parse-book file)
          book-db (book->db book)

          book-exists? (seq (db/one db/conn (book-q (:id book-db))))]

    book-exists?
    {:status  301
     :headers {"Location" "/?message=book-duplicate"}}

    :let [progress-id (:id (db/one db/conn (init-progress-q)))]

    :do (future
          (with-on-sentence-processed-cb
            save-progress!
            (process-book! progress-id book-db)))

    {:status 200
     :body   (html (fragments/progress-bar progress-id 100))}))
