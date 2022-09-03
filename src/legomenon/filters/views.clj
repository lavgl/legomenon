(ns legomenon.filters.views
  (:require [legomenon.utils :as utils]))


(defn filter-button [{:keys [label href is-active]}]
  [:a {:href  href
       :class (utils/classname {:badge             :always
                                :rounded-pill      :always
                                :top-words-filter  :always
                                :text-bg-info      (not is-active)
                                :text-bg-secondary is-active})}
   label])


(defn top-words-panel [book-id what-to-show {:keys [make-href]}]
  [:div.top-words-filter-panel
   (filter-button {:label     "Show all"
                   :href      (make-href book-id nil)
                   :is-active (nil? what-to-show)})
   (filter-button {:label     "Show top 4k"
                   :href      (make-href book-id "top-4k")
                   :is-active (= what-to-show "top-4k")})
   (filter-button {:label     "Show top 10k"
                   :href      (make-href book-id "top-10k")
                   :is-active (= what-to-show "top-10k")})])
