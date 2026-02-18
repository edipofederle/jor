(ns jor.views.toolbar
  (:require [reagent.core :as r]
            [re-frame.core :as rf]
            [jor.scene.three :as three]))

(defonce ^:private anim-playing? (r/atom false))
(defonce ^:private dims-on?      (r/atom false))

(defn view []
  (let [explode-f  @(rf/subscribe [:explode-factor])
        _active-id @(rf/subscribe [:active-joint-id])   ; ensures re-render on every joint switch
        has-dims?  @(rf/subscribe [:active-joint-has-dims?])
        playing?   @anim-playing?
        dims?      @dims-on?
        ;; Sync local atom: clear-dims! may have been called externally (joint switch)
        _          (when (and dims? (not (three/dims-showing?)))
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
