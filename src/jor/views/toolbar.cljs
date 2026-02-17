(ns jor.views.toolbar
  (:require [reagent.core :as r]
            [re-frame.core :as rf]
            [jor.scene.three :as three]))

(defonce ^:private anim-playing? (r/atom false))

(defn view []
  (let [explode-f @(rf/subscribe [:explode-factor])
        playing?  @anim-playing?]
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
                           ;; When stopping, sync final animated position back to re-frame
                           (when-not @anim-playing?
                             (rf/dispatch [:set-explode-factor (three/anim-factor)])))}
      (if playing? "⏹ stop" "▶ animate")]]))
