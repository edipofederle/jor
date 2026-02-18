(ns jor.views.sidebar
  (:require [re-frame.core :as rf]
            [jor.joints.registry :as registry]))

(defn joint-button [joint active-id]
  [:button.joint-btn
   {:class    (when (= (:id joint) active-id) "active")
    :on-click #(rf/dispatch [:select-joint (:id joint)])}
   (:label joint)])

(defn view []
  (let [active-id   @(rf/subscribe [:active-joint-id])
        active      @(rf/subscribe [:active-joint])
        params      @(rf/subscribe [:merged-joint-params])
        cut-seq     @(rf/subscribe [:cut-seq])
        active-step @(rf/subscribe [:cut-step])]
    [:div.sidebar
     [:div.sidebar-section
      [:h2 "Joints"]
      (for [joint (registry/all-joints)]
        ^{:key (:id joint)}
        [joint-button joint active-id])]
     (when active
       [:<>
        [:div.sidebar-section
         (when-let [img (:image active)]
           [:img.joint-photo
            {:src      img
             :alt      (:label active)
             :on-error (fn [e] (set! (.. e -target -style -display) "none"))}])
         [:h2 "About"]
         [:p.joint-doc (:doc active)]]
        (when-let [derived-fn (:derived-fn active)]
          [:div.sidebar-section
           [:h2 "Key measurements"]
           [:table.params-table
            [:tbody
             (for [[lbl val] (derived-fn params)]
               ^{:key lbl}
               [:tr
                [:td.param-name lbl]
                [:td.param-val val]])]]])
        (when cut-seq
          (let [part-labels (into {} (map (juxt :id :label) (:parts active)))
                groups      (partition-by :part cut-seq)]
            [:div.sidebar-section
             [:h2 "Steps"]
             (for [group groups]
               (let [part (:part (first group))]
                 ^{:key (str part)}
                 [:div.step-group
                  [:div.step-group-header
                   (if part (get part-labels part) "Both parts")]
                  [:ol.cut-seq
                   (for [{:keys [step label] :as s} group]
                     ^{:key step}
                     [:li.cut-step
                      {:class    (when (= step (:step active-step)) "active")
                       :on-click #(rf/dispatch [:set-cut-step s])}
                      [:span.cut-step-num step]
                      [:span.cut-step-label label]])]]))]))
        ])]))
