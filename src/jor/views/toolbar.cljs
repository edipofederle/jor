(ns jor.views.toolbar
  (:require [reagent.core :as r]
            [re-frame.core :as rf]
            [jor.scene.three :as three]))

(defonce ^:private anim-playing? (r/atom false))
(defonce ^:private dims-on?      (r/atom false))

(defn view []
  (let [explode-f  @(rf/subscribe [:explode-factor])
        has-dims?  @(rf/subscribe [:active-joint-has-dims?])
        playing?   @anim-playing?
        dims?      @dims-on?
        ;; Auto-clear dims when switching to a joint that doesn't support them
        _          (when (and dims? (not has-dims?))
                     (three/toggle-dims!)
                     (reset! dims-on? false))]
    [:div.toolbar
     [:h1 "jor"]
     [:label "explode"]
     [:input {:type      "range"
              :min       0
              :max       1
              :step      0.01
              :value     explode-f
              :disabled  playing?
              :on-change #(rf/dispatch [:set-explode-factor
                                        (js/parseFloat (.. % -target -value))])}]
     [:button {:on-click #(rf/dispatch [:reset-explode])
               :disabled playing?} "assemble"]
     [:button {:on-click #(three/reset-camera!)} "reset view"]
     [:button {:class    (when playing? "active")
               :on-click (fn []
                           (three/toggle-anim!)
                           (swap! anim-playing? not)
                           (when-not @anim-playing?
                             (rf/dispatch [:set-explode-factor (three/anim-factor)])))}
      (if playing? "⏹ stop" "▶ animate")]
     [:button {:class    (when (and has-dims? dims?) "active")
               :disabled (not has-dims?)
               :title    (if has-dims? "Toggle dimension lines" "No dimensions for this joint")
               :on-click (fn []
                           (three/toggle-dims!)
                           (swap! dims-on? not))}
      "dims"]]))
