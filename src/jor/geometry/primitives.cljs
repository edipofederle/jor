(ns jor.geometry.primitives
  (:require ["three" :as THREE]))

(defn box-geo
  "Returns a BoxGeometry of width w, height h, depth d."
  [w h d]
  (THREE/BoxGeometry. w h d))

(defn mesh
  "Returns a THREE.Mesh from geometry + material."
  [geo mat]
  (THREE/Mesh. geo mat))

(defn set-pos!
  "Sets the position of a Three.js object in-place and returns it."
  [obj x y z]
  (.set (.-position obj) x y z)
  obj)

(defn group
  "Returns an empty THREE.Group."
  []
  (THREE/Group.))

(defn add-to!
  "Adds children to parent Three.js object. Returns parent."
  [parent & children]
  (doseq [c children]
    (.add parent c))
  parent)

(defn make-box-mesh
  "Convenience: creates a mesh from BoxGeometry and a material."
  [w h d mat]
  (mesh (box-geo w h d) mat))
