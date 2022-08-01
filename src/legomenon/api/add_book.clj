(ns legomenon.api.add-book
  (:refer-clojure :exclude [number?])
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [hiccup.core :refer [html]]
            [mount.core :as mount]
            [pantomime.extract :as extract]
            [dk.simongray.datalinguist :as nlp]
            [tick.core :as t]

            [legomenon.db :as db]
            [legomenon.utils :as utils]
            [legomenon.uploading-status.vars :as us.vars]
            [legomenon.uploading-status.views :as us.views]
            [legomenon.uploading-status.dal :as us.dal]))


(defn insert-book-q [book]
  {:insert-into [:books]
   :values      [book]})


(defn book-q [id]
  {:select [:id]
   :from   [:books]
   :where  [:= :id id]})


(defn init-status-q [filename]
  {:insert-into [:uploading_status]
   :values      [{:state    us.vars/STATE-STEP-1
                  :filename filename}]
   :returning   [:id]})


(defn mark-book-upload-finished-at [book-id]
  {:update :books
   :set    {:upload_finished_at (t/now)}
   :where  [:= :id book-id]})


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


(defn save-progress! [status-id {:keys [current total]}]
  (let [last-write (get @progress-timer status-id)
        second-ago (t/<< (t/now) (t/of-seconds 1))]
    (when (or (nil? last-write)
              (t/< last-write second-ago))
      (let [percent (int (Math/floor (* 100 (/ current total))))]
        (us.dal/update! status-id {:state_info {:current_percent percent}})
        (swap! progress-timer assoc status-id (t/now))))))


(defn cleanup-progress [status-id]
  (swap! progress-timer dissoc status-id))


(defn lemma-frequencies [text]
  (let [sentenses (->> (remove-explicit-line-breaks text)
                       (split-sentences))
        xf        (comp
                    (map-indexed (fn [i s]
                                   (when *on-sentence-processed-cb*
                                     (*on-sentence-processed-cb*
                                       {:current i
                                        :total   (count sentenses)}))
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


(defn process-book! [book]
  (let [lemmas    (lemma-frequencies (:text book))
        lemmas-db (lemmas->db lemmas (:id book))]
    (db/execute db/conn (insert-lemmas-q lemmas-db))))


(defn process-book-task [{:keys [file status-id]}]
  (try
    (let [book (parse-book file)
          book (book->db book)

          ;; TODO: don't pass db/conn as `q`/`one` arguments
          book-exists? (seq (db/one db/conn (book-q (:id book))))]
      (if book-exists?
        (us.dal/update! status-id {:state      us.vars/STATE-ERROR
                                   :state_info {:error_code 1}})
        (do
          (db/execute db/conn (insert-book-q book))
          (us.dal/update! status-id {:state      us.vars/STATE-STEP-2
                                     ;; NOTE: :current_percent is 2 just to not make it empty,
                                     ;; so progress bar is visually understandable at that moment
                                     :state_info {:current_percent 2}})

          (binding [*on-sentence-processed-cb* #(save-progress! status-id %)]
            (process-book! book)
            (cleanup-progress status-id))
          (db/execute db/conn (mark-book-upload-finished-at (:id book)))
          (us.dal/update! status-id {:state      us.vars/STATE-DONE
                                     :state_info nil}))))
    (catch Exception e
      (us.dal/update! status-id {:state      us.vars/STATE-ERROR
                                 :state_info {:error (prn-str e)}}))))


(defn handler [req]
  (let [file      (-> req :params :file)
        filename  (:filename file)
        status-id (:id (db/one db/conn (init-status-q filename)))]

    (future (process-book-task {:status-id status-id
                                :file      file}))
    {:status 200
     :body   (html (us.views/uploading-status status-id
                     {:state    us.vars/STATE-STEP-1
                      :filename filename}))}))
