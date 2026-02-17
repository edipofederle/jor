(ns jor.views.toolbar
  (:require [re-frame.core :as rf]
            [jor.scene.three :as three]))

(defn view []
  (let [explode-f @(rf/subscribe [:explode-factor])]
    [:div.toolbar
     [:h1 "jor"]
     [:label "explode"]
     [:input {:type      "range"
              :min       0
              :max       1
              :step      0.01
              :value     explode-f
              :on-change #(rf/dispatch [:set-explode-factor
                                        (js/parseFloat (.. % -target -value))])}]
     [:button {:on-click #(rf/dispatch [:reset-explode])} "assemble"]
     [:button {:on-click #(three/reset-camera!)} "reset view"]]))
