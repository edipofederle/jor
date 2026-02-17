(ns jor.views.sidebar
  (:require [re-frame.core :as rf]
            [jor.joints.registry :as registry]))

(defn joint-button [joint active-id]
  [:button.joint-btn
   {:class    (when (= (:id joint) active-id) "active")
    :on-click #(rf/dispatch [:select-joint (:id joint)])}
   (:label joint)])

(defn view []
  (let [active-id @(rf/subscribe [:active-joint-id])
        active    @(rf/subscribe [:active-joint])]
    [:div.sidebar
     [:div.sidebar-section
      [:h2 "Joints"]
      (for [joint (registry/all-joints)]
        ^{:key (:id joint)}
        [joint-button joint active-id])]
     (when active
       [:div.sidebar-section
        [:h2 "About"]
        [:p.joint-doc (:doc active)]])]))
