(ns jor.events.viewport
  (:require [re-frame.core :as rf]))

(rf/reg-event-db
 :set-explode-factor
 (fn [db [_ factor]]
   (assoc-in db [:viewport :explode-factor] factor)))

(rf/reg-event-db
 :reset-explode
 (fn [db _]
   (assoc-in db [:viewport :explode-factor] 0.0)))
