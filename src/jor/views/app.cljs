(ns jor.views.app
  (:require [reagent.core :as r]
            [re-frame.core :as rf]
            [jor.scene.three :as three]
            [jor.views.sidebar :as sidebar]
            [jor.views.toolbar :as toolbar]))

;; ── Canvas component ─────────────────────────────────────────────────────────
;; r/dom-node was removed in Reagent 1.x. We use a callback ref instead.

(defn- canvas []
  (let [canvas-ref   (atom nil)
        resize-obs   (atom nil)
        initialized? (atom false)
        sub-refs     (atom nil)]
    (r/create-class
     {:display-name "Canvas"

      :component-did-mount
      (fn [_this]
        (when-let [^js el @canvas-ref]
          (three/init! el)
          (three/rebuild-joint! @(rf/subscribe [:active-joint])
                                @(rf/subscribe [:merged-joint-params])
                                @(rf/subscribe [:explode-factor]))
          (three/start-loop!)
          (reset! initialized? true)
          ;; Watch subscriptions directly so REPL dispatches also trigger rebuilds.
          ;; Debounce via setTimeout 0 so that when a single dispatch changes
          ;; multiple subscriptions (j, p, f all fire), we only rebuild once.
          (let [j       (rf/subscribe [:active-joint])
                p       (rf/subscribe [:merged-joint-params])
                f       (rf/subscribe [:explode-factor])
                timer   (atom nil)
                rebuild! (fn [& _]
                           (js/clearTimeout @timer)
                           (reset! timer
                                   (js/setTimeout #(three/rebuild-joint! @j @p @f) 0)))]
            (add-watch j ::scene rebuild!)
            (add-watch p ::scene rebuild!)
            (add-watch f ::scene rebuild!)
            (reset! sub-refs [j p f]))
          (let [^js obs (js/ResizeObserver.
                         (fn [^js entries]
                           (when-let [^js entry (first entries)]
                             (let [^js cr (.-contentRect entry)]
                               (three/resize! (.-width cr) (.-height cr))))))]
            (.observe obs (.-parentElement el))
            (reset! resize-obs obs))))

      :component-will-unmount
      (fn [_this]
        (three/stop-loop!)
        (doseq [sub @sub-refs]
          (remove-watch sub ::scene))
        (when-let [^js obs @resize-obs]
          (.disconnect obs)))

      :reagent-render
      (fn []
        [:canvas {:ref #(reset! canvas-ref %)}])})))

;; ── Root layout ───────────────────────────────────────────────────────────────

(defn root []
  [:div#jor-app
   [toolbar/view]
   [:div.layout
    [sidebar/view]
    [:div.canvas-wrap
     [canvas]]]])
