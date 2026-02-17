(ns jor.joints.registry
  (:require [jor.joints.dovetail      :as dovetail]
            [jor.joints.box-finger    :as box-finger]
            [jor.joints.mortise-tenon :as mortise-tenon]
            [jor.joints.half-lap      :as half-lap]
            [jor.joints.bridle        :as bridle]))

(def joints
  {:dovetail      dovetail/definition
   :box-finger    box-finger/definition
   :mortise-tenon mortise-tenon/definition
   :half-lap      half-lap/definition
   :bridle        bridle/definition})

;; Ordered list for sidebar display
(def joint-order
  [:dovetail :box-finger :mortise-tenon :half-lap :bridle])

(defn get-joint [id]
  (get joints id))

(defn all-joints []
  (mapv #(get joints %) joint-order))
