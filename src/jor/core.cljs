(ns jor.core
  (:require [reagent.dom :as rdom]
            [re-frame.core :as rf]
            [jor.db :as db]
            ;; Register events and subs (side-effects on load)
            [jor.events.joints]
            [jor.events.viewport]
            [jor.subs.joints]
            [jor.subs.viewport]
            ;; Root view
            [jor.views.app :as app]))

(defn mount-root []
  (rdom/render [app/root] (.getElementById js/document "app")))

(defn ^:dev/after-load on-reload []
  (rf/clear-subscription-cache!)
  (mount-root))

(defn ^:export main []
  (rf/dispatch-sync [:initialize-db db/default-db])
  (mount-root))

(defonce ^:private _auto-start (main))
