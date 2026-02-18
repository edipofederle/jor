(ns jor.views.sidebar
  (:require [reagent.core :as r]
            [re-frame.core :as rf]
            [jor.joints.registry :as registry]))

;; ── Tool image lookup ─────────────────────────────────────────────────────────

(def ^:private tool-images
  {"Marking gauge"  "https://upload.wikimedia.org/wikipedia/commons/thumb/0/07/Marking_gauges.jpg/330px-Marking_gauges.jpg"
   "Bench chisel"   "https://upload.wikimedia.org/wikipedia/commons/thumb/a/ae/Neolithic_chisels_4100-2700_BC.jpg/330px-Neolithic_chisels_4100-2700_BC.jpg"
   "Mortise chisel" "https://upload.wikimedia.org/wikipedia/commons/thumb/a/ae/Neolithic_chisels_4100-2700_BC.jpg/330px-Neolithic_chisels_4100-2700_BC.jpg"
   "Tenon saw"      "https://upload.wikimedia.org/wikipedia/commons/thumb/1/18/Backsaws.jpg/330px-Backsaws.jpg"
   "Dovetail saw"   "https://upload.wikimedia.org/wikipedia/commons/thumb/1/18/Backsaws.jpg/330px-Backsaws.jpg"
   "Coping saw"     "https://upload.wikimedia.org/wikipedia/commons/thumb/2/2d/Coping_saw_2.jpg/330px-Coping_saw_2.jpg"
   "Router plane"   "https://upload.wikimedia.org/wikipedia/commons/thumb/e/e7/Veritas_router_plane.jpg/330px-Veritas_router_plane.jpg"
   "Shoulder plane" "https://upload.wikimedia.org/wikipedia/commons/thumb/d/d8/BullnoseShoulderPlane.jpg/330px-BullnoseShoulderPlane.jpg"
   "Sliding bevel"  "https://upload.wikimedia.org/wikipedia/commons/thumb/e/ea/T_bevel.JPG/330px-T_bevel.JPG"
   "Mallet" "https://upload.wikimedia.org/wikipedia/commons/a/ae/Wooden_mallet.jpg"})

;; Tracks the currently hovered tool: {:name "..." :top <px>} or nil.
;; defonce so hot-reload doesn't reset it mid-hover.
(defonce ^:private hovered-tool (r/atom nil))

;; ── Tool tooltip (position: fixed — escapes sidebar overflow clipping) ────────

(defn- tool-tooltip []
  (when-let [{:keys [name top]} @hovered-tool]
    (when-let [url (get tool-images name)]
      [:div.tool-tooltip {:style {:top top}}
       [:img {:src url :alt name}]
       [:span.tool-tooltip-name name]])))

;; ── Components ────────────────────────────────────────────────────────────────

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
     [tool-tooltip]
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
        (when-let [tools (seq (:tools active))]
          [:div.sidebar-section
           [:h2 "Hand Tools"]
           [:ul.tools-list
            (for [t tools]
              ^{:key t}
              [:li
               {:on-mouse-enter (fn [e]
                                  (let [rect (.getBoundingClientRect (.-currentTarget e))]
                                    (reset! hovered-tool {:name t :top (.-top rect)})))
                :on-mouse-leave #(reset! hovered-tool nil)}
               t])]])
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
