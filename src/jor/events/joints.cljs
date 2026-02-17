(ns jor.events.joints
  (:require [re-frame.core :as rf]))

(rf/reg-event-db
 :initialize-db
 (fn [_ [_ db]] db))

(rf/reg-event-db
 :select-joint
 (fn [db [_ joint-id]]
   (-> db
       (assoc :active-joint-id joint-id)
       (assoc-in [:animation :cut-step] 0)
       (assoc-in [:animation :playing?] false))))

(rf/reg-event-db
 :set-joint-param
 (fn [db [_ joint-id param-key value]]
   (assoc-in db [:joint-params joint-id param-key] value)))

(rf/reg-event-db
 :reset-joint-params
 (fn [db [_ joint-id]]
   (update db :joint-params dissoc joint-id)))
