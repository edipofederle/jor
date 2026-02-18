(ns jor.geometry.dimensions
  (:require ["three" :as THREE]
            ["three/addons/renderers/CSS2DRenderer.js" :refer [CSS2DObject]]))

;; Shared material for all dim lines (created once).
(defonce ^:private dim-mat
  (THREE/LineBasicMaterial. #js {:color 0x7799cc}))

(defn- label-el [text]
  (let [el (js/document.createElement "div")]
    (set! (.-className el) "dim-label")
    (set! (.-textContent el) text)
    el))

(defn- line-seg
  "Single Three.js Line from (x1,y1,z1) to (x2,y2,z2)."
  [x1 y1 z1 x2 y2 z2]
  (let [pts (js/Float32Array. #js [x1 y1 z1 x2 y2 z2])]
    (THREE/Line.
     (doto (THREE/BufferGeometry.)
       (.setAttribute "position" (THREE/BufferAttribute. pts 3)))
     dim-mat)))

(defn make-dim!
  "Build one dimension annotation group.
   spec keys:
     :from        [x y z]  — point on geometry at start of measured extent
     :to          [x y z]  — point on geometry at end of measured extent
     :offset-dir  [x y z]  — unit direction to push the dim line away from geometry
     :offset-dist number   — how far to offset (mm)
     :label       string   — text to display (e.g. \"200mm\")"
  [{:keys [from to offset-dir offset-dist label]}]
  (let [grp (THREE/Group.)
        [x1 y1 z1] from
        [x2 y2 z2] to
        [ox oy oz] offset-dir
        ;; Offset the two endpoints away from the geometry
        ax (+ x1 (* ox offset-dist))
        ay (+ y1 (* oy offset-dist))
        az (+ z1 (* oz offset-dist))
        bx (+ x2 (* ox offset-dist))
        by (+ y2 (* oy offset-dist))
        bz (+ z2 (* oz offset-dist))
        ;; CSS2D label sits at the midpoint of the offset line
        lbl (CSS2DObject. (label-el label))]
    (.set (.-position lbl)
          (/ (+ ax bx) 2)
          (/ (+ ay by) 2)
          (/ (+ az bz) 2))
    ;; Main dim line
    (.add grp (line-seg ax ay az bx by bz))
    ;; Extension lines from geometry surface to the dim line
    (.add grp (line-seg x1 y1 z1 ax ay az))
    (.add grp (line-seg x2 y2 z2 bx by bz))
    (.add grp lbl)
    grp))

(defn build-dims!
  "Given a sequence of dim specs, returns a THREE.Group of all annotations."
  [specs]
  (reduce (fn [^js g spec] (doto g (.add (make-dim! spec))))
          (THREE/Group.)
          specs))
