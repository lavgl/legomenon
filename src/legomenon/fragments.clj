(ns legomenon.fragments
  (:require [clojure.string :as str]
            [hiccup.page :as hiccup.page]))


(defn page [header & content]
  (hiccup.page/html5 {:encoding "UTF-8"}
    [:head {}
     [:meta {:charset "UTF-8"}]
     [:meta {:name    "viewport"
             :content "width=device-width, initial-scale=1"}]

     [:link {:href        "https://unpkg.com/bootstrap@5.2.0/dist/css/bootstrap.min.css"
             :rel         "stylesheet"
             :type        "text/css"
             :crossorigin "anonymous"}]
     (hiccup.page/include-css "/public/css/main.css")]
    [:body
     (when header
       [:div.header-divider
        header])
     [:div.container
      content]
     [:sc]

     ;; htmx
     [:script {:src         "https://unpkg.com/htmx.org@1.8.0"
               :integrity   "sha384-cZuAZ+ZbwkNRnrKi05G/fjBX+azI9DNOkNYysZ0I/X5ZFgsmMiBXgDZof30F5ofc"
               :crossorigin "anonymous"}]

     ;; bootstrap
     [:script {:src         "https://unpkg.com/bootstrap@5.2.0/dist/js/bootstrap.bundle.min.js"
               :integrity   "sha384-A3rJD856KowSb7dwlZdYEkO39Gagi7vIsF0jrRAoQmDKKtQBHUuLZ9AsSv4jD4Xa"
               :crossorigin "anonymous"}]

     ;; hyperscript
     [:script {:src "https://unpkg.com/hyperscript.org@0.9.7"}]

     [:script
      "
htmx.onLoad(element => {
  if (element.tagName == 'TR' && element.nextSibling && element.nextSibling.tagName == 'TR') {
    element.nextSibling.focus();
  }});
"]]))


(defn navbar [req]
  (let [page              (-> req :reitit.core/match :data :get :page)
        book-id           (-> req :path-params :id)
        back-btn          (case page
                            (:add-book :book-dict) [:a.nav-link {:href "/"} "Go back"]
                            [:a.nav-link "Legomenon"])
        book-dict-btn     (case page
                            (:book-text :book-settings)
                            [:a.nav-link {:href (format "/books/%s/" book-id)} "Words"]
                            nil)
        book-text-btn     (case page
                            (:book-dict :book-settings)
                            [:a.nav-link {:href (format "/books/%s/text/" book-id)} "Text"]
                            nil)
        book-settings-btn (case page
                            (:book-dict :book-text)
                            [:a.nav-link {:href (format "/books/%s/settings/" book-id)} "Settings"]
                            nil)]
    [:nav.navbar.navbar-dark.navbar-expand-lg
     [:div.container-fluid
      back-btn
      [:button.navbar-toggler {:type           "button"
                               :data-bs-toggle "collapse"
                               :data-bs-target "#navbarNav"
                               :aria-controls  "navbarNav"
                               :aria-expanded  "false"
                               :aria-label     "Toggle navigation"}
       [:span.navbar-toggler-icon]]
      [:div#navbarNav.collapse.navbar-collapse
       [:div.navbar-nav
        [:a.nav-link {:href  "/"
                      :class (when (= page :books-list) "active")} "Books List"]
        ;; [:a.nav-link {:href  "/books/add/"
        ;;               :class (when (= page :add-book) "active")} "Add Book"]
        book-dict-btn
        book-text-btn
        book-settings-btn]]]]))


(defn render-row [{:keys [keys-allowed list]}
                  {:keys [lemma count id occur-rate]}]
  (assert keys-allowed)
  (let [hx-trigger (->> keys-allowed
                        (map #(format "key=='%s'" %))
                        (str/join " || ")
                        (format "keyup[%s], swipe"))]
    [:tr
     {:class        (str "dict-word " list)
      :data-word-id id
      :tabindex     "0"
      :hx-trigger   hx-trigger
      :hx-post      "/api/words/op/"
      :hx-vals      (format "js:{key: event.key, id: '%s', event: event.type, direction: event.detail.direction}" id)
      :hx-swap      "outerHTML"}
     [:td lemma]
     [:td count]
     [:td occur-rate]]))


(def render-known-row     (partial render-row {:list "known" :keys-allowed ["u"]}))
(def render-trash-row     (partial render-row {:list "trash" :keys-allowed ["u"]}))
(def render-postponed-row (partial render-row {:list "postponed" :keys-allowed ["u"]}))
(def render-memo-row      (partial render-row {:list "memo" :keys-allowed ["u" "k"]}))
(def render-plain-row     (partial render-row {:keys-allowed ["k" "t" "m" "p"]}))


(defn render-op-row [word]
  [:tr
   [:td {:colspan 2}
    [:div.btn-group.btn-group-lg.w-100
     ;; TODO: dont fuck myself, make util for clj->str conversion
     ;; TODO: make normal api, dont use this 'key: t' shit
     [:button.btn.btn-outline-info {:hx-post   "/api/words/op/"
                                    :hx-vals   (format "js:{event: 'keyup', key: 't', id: '%s'}" (:id word))
                                    :hx-target "closest tr"
                                    :hx-swap   "outerHTML"}
      "T"]
     [:button.btn.btn-outline-info {:hx-post   "/api/words/op/"
                                    :hx-vals   (format "js:{event: 'keyup', key: 'k', id: '%s'}" (:id word))
                                    :hx-target "closest tr"
                                    :hx-swap   "outerHTML"}
      "K"]
     [:button.btn.btn-outline-info {:hx-post   "/api/words/op/"
                                    :hx-vals   (format "js:{event: 'keyup', key: 'm', id: '%s'}" (:id word))
                                    :hx-target "closest tr"
                                    :hx-swap   "outerHTML"}
      "M"]
     [:button.btn.btn-outline-info {:hx-post   "/api/words/op/"
                                    :hx-vals   (format "js:{event: 'keyup', key: 'p', id: '%s'}" (:id word))
                                    :hx-target "closest tr"
                                    :hx-swap   "outerHTML"}
      "P"]
     [:button.btn.btn-outline-info {:hx-post   "/api/words/op/"
                                    :hx-vals   (format "js:{event: 'keyup', key: 'u', id: '%s'}" (:id word))
                                    :hx-target "closest tr"
                                    :hx-swap   "outerHTML"}
      "╳"]]]])
