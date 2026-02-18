(ns jor.events.joints
  (:require [re-frame.core :as rf]
            [jor.scene.three :as three]))

(rf/reg-event-db
 :initialize-db
 (fn [_ [_ db]] db))

(rf/reg-event-db
 :select-joint
 (fn [db [_ joint-id]]
   (three/highlight-step! nil)
   (-> db
       (assoc :active-joint-id joint-id)
       (assoc-in [:animation :cut-step] nil)
       (assoc-in [:animation :playing?] false))))

(rf/reg-event-db
 :set-cut-step
 (fn [db [_ step]]
   ;; Clicking the active step deselects it
   (let [current  (get-in db [:animation :cut-step])
         new-step (when (not= (:step step) (:step current)) step)]
     (three/highlight-step! (:part new-step))
     (assoc-in db [:animation :cut-step] new-step))))

(rf/reg-event-db
 :set-joint-param
 (fn [db [_ joint-id param-key value]]
   (assoc-in db [:joint-params joint-id param-key] value)))

(rf/reg-event-db
 :reset-joint-params
 (fn [db [_ joint-id]]
   (update db :joint-params dissoc joint-id)))
