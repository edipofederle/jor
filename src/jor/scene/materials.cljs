(ns jor.scene.materials
  (:require ["three" :as THREE]))

;; Lazily-initialised material cache so we don't create GL objects at
;; namespace load time (which may precede renderer creation).
(defonce ^:private cache (atom nil))

(defn- init! []
  (reset! cache
          {:wood-light (THREE/MeshLambertMaterial. #js {:color 0xC8956C})
           :wood-dark  (THREE/MeshLambertMaterial. #js {:color 0x7B4B2A})
           :highlight  (THREE/MeshLambertMaterial. #js {:color 0xFFAA44})}))

(defn get-mat
  "Returns the cached THREE.Material for key k.
   Valid keys: :wood-light :wood-dark :highlight"
  [k]
  (when (nil? @cache) (init!))
  (get @cache k))

(defn reset-cache!
  "Call on hot-reload to force re-creation of materials."
  []
  (reset! cache nil))
