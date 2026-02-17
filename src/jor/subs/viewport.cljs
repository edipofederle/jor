(ns jor.subs.viewport
  (:require [re-frame.core :as rf]))

(rf/reg-sub
 :explode-factor
 (fn [db _]
   (get-in db [:viewport :explode-factor])))
