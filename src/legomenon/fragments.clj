(ns legomenon.fragments
  (:require [hiccup.page :as hiccup.page]))


(defn page [header & content]
  (hiccup.page/html5 {:encoding "UTF-8"}
                     [:head {}
                      [:meta {:charset "UTF-8"}]
                      [:meta {:name    "viewport"
                              :content "width=device-width, initial-scale=1"}]
                      (hiccup.page/include-css
       ;; "/public/css/normalize.css"
                       "/public/css/bootstrap.min.css"
                       "/public/css/main.css")]
                     [:body
                      (when header
                        [:div.header-divider
                         header])
                      [:div.container
                       content]
                      (hiccup.page/include-js
                       "/public/js/htmx.js"
                       "/public/js/bootstrap.bundle.min.js"
                       "/public/js/_hyperscript_web.min.js"
                       "/public/js/swipe.js")
                      [:script
                       "
htmx.onLoad(element => {
  if (element.tagName == 'TR' && element.nextSibling && element.nextSibling.tagName == 'TR') {
    element.nextSibling.focus();
  }});
swipe.init('swipable', 100);
"]]))


(defn navbar [req]
  (let [page          (-> req :reitit.core/match :data :get :page)
        back-btn      (case page
                        (:add-book :book-dict) [:a.nav-link {:href "/"} "Go back"]
                        [:a.nav-link "Legomenon"])
        book-text-btn (case page
                        :book-dict (let [book-id (-> req :path-params :id)]
                                     [:a.nav-link {:href (format "/books/%s/text/" book-id)} "This Book Text"])
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
        [:a.nav-link {:href  "/books/add/"
                      :class (when (= page :add-book) "active")} "Add Book"]
        book-text-btn]]]]))
