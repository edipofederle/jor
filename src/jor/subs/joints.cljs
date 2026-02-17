(ns jor.subs.joints
  (:require [re-frame.core :as rf]
            [jor.joints.registry :as registry]))

(rf/reg-sub
 :active-joint-id
 (fn [db _] (:active-joint-id db)))

(rf/reg-sub
 :active-joint
 :<- [:active-joint-id]
 (fn [joint-id _]
   (registry/get-joint joint-id)))

(rf/reg-sub
 :joint-params
 ;; Returns fully-merged params: joint defaults + any user overrides
 :<- [:active-joint]
 :<- [:active-joint-id]
 (fn [[joint overrides-map] [_ joint-id]]
   ;; overrides-map here is actually the raw :joint-params db slice
   ;; Correctly pull it via a separate sub
   (:params joint)))

;; Raw override map from db — used by the merged-params sub
(rf/reg-sub
 :joint-param-overrides
 (fn [db _] (:joint-params db)))

(rf/reg-sub
 :merged-joint-params
 :<- [:active-joint]
 :<- [:active-joint-id]
 :<- [:joint-param-overrides]
 (fn [[joint joint-id overrides] _]
   (merge (:params joint)
          (get overrides joint-id {}))))
