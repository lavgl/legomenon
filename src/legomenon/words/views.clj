(ns legomenon.words.views
  (:require [legomenon.fragments :as fragments]
            [legomenon.words.helpers :as words.helpers]))


(defn render-table-body [words]
  (let [total-words-count (->> words (map :count) (apply +))]
    (->> (words.helpers/assign-occur-rate-to-first-by-group total-words-count words)
         (map (fn [word]
                (case (:list word)
                  "known"
                  (fragments/render-known-row word)

                  "trash"
                  (fragments/render-trash-row word)

                  "memo"
                  (fragments/render-memo-row word)

                  "postponed"
                  (fragments/render-postponed-row word)

                  (fragments/render-plain-row word)))))))


(defn words-table [words]
[:table
 {:_ "
init
  set :dx_to_hightlight to 50
  set :dx_to_be_swiped to 50


on touchstart from <tr/>
  set :x to event.changedTouches[0].screenX
  set :y to event.changedTouches[0].screenY
  set :state to 'none'


on touchmove from <tr/>
  if :state is 'scrolling' exit end

  set :dx to event.changedTouches[0].screenX - :x
  set :dy to event.changedTouches[0].screenY - :y
  set :tr to the closest <tr/> to the event.target

  if :state is 'none' and Math.abs(:dy) > Math.abs(:dx)
    set :state to 'scrolling'
  else
    set :state to 'swiping'
  end

  if :state is 'swiping'
    halt the event
  end

  if :dx > :dx_to_hightlight add .swiping to the :tr
  else remove .swiping from the :tr end


on touchend
  if :state is 'swiping' and :dx > :dx_to_be_swiped
    set word_id to @data-word-id of :tr
    fetch `/fragments/op-row/?word-id=${word_id}`
    then put the result into :tr's outerHTML
    then htmx.process(document.body)
  end

  if :tr exists
   remove .swiping from the :tr then
  end

  set :state to 'none'
"}
       [:thead
        [:tr.dict-word [:th "Word"] [:th "Count"] [:th "Occurrence rate"]]]
       [:tbody (render-table-body words)]])
