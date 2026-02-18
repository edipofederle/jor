(ns jor.joints.sliding-dovetail
  (:require [jor.geometry.primitives :as p]
            [jor.scene.materials :as m]))

;; Sliding Dovetail joint:
;; Housing board runs along Y (vertical). A horizontal groove is cut into
;; its +Z face running the full X width. The tail board (shelf) runs along X
;; and its tongue slides into the groove from the +X side.
;;
;;  +Z face of housing:         Assembly (top view, Y is up):
;;  ┌────────────────────┐      housing (vertical)
;;  │                    │         ↕
;;  ├━━━━━━━━━━━━━━━━━━━━┤  ←groove ════╪════════════════ ← tail board →
;;  │                    │
;;  └────────────────────┘

(def default-params
  {:housing-width    80   ; X total width of the housing board face
   :housing-height  120   ; Y total height of the housing board
   :housing-depth    25   ; Z thickness of the housing board
   :groove-height    20   ; Y height of the groove (= tail board thickness)
   :groove-depth     12   ; Z depth of groove into the housing face
   :tail-body-length 80   ; X how far the tail board extends beyond the housing
   })

(defn- housing
  [{:keys [housing-width housing-height housing-depth groove-height groove-depth]}]
  (let [mat    (m/get-mat :wood-dark)
        grp    (p/group)
        hh     (/ housing-height 2)
        hd     (/ housing-depth 2)
        gh     (/ groove-height 2)
        slab-h (- hh gh)
        back-d (- housing-depth groove-depth)
        ;; Slab above the groove
        top    (p/set-pos! (p/make-box-mesh housing-width slab-h housing-depth mat)
                           0 (+ gh (/ slab-h 2)) 0)
        ;; Slab below the groove
        bot    (p/set-pos! (p/make-box-mesh housing-width slab-h housing-depth mat)
                           0 (- (+ gh (/ slab-h 2))) 0)
        ;; Material behind the groove opening
        back   (p/set-pos! (p/make-box-mesh housing-width groove-height back-d mat)
                           0 0 (- (/ groove-depth 2)))]
    (p/add-to! grp top bot back)
    grp))

(defn- tail
  [{:keys [housing-width housing-depth groove-height groove-depth tail-body-length]}]
  (let [mat    (m/get-mat :wood-light)
        grp    (p/group)
        hw     (/ housing-width 2)
        hd     (/ housing-depth 2)
        ;; Tongue: exactly fills the groove
        tongue (p/set-pos! (p/make-box-mesh housing-width groove-height groove-depth mat)
                           0 0 (- hd (/ groove-depth 2)))
        ;; Body: extends in +X from the housing, full housing depth in Z
        body   (p/set-pos! (p/make-box-mesh tail-body-length groove-height housing-depth mat)
                           (+ hw (/ tail-body-length 2)) 0 0)]
    (p/add-to! grp tongue body)
    grp))

(defn- build-dims
  [{:keys [housing-width housing-height housing-depth groove-height groove-depth tail-body-length]}]
  (let [hw  (/ housing-width 2)
        hh  (/ housing-height 2)
        hd  (/ housing-depth 2)
        gh  (/ groove-height 2)]
    {:housing
     [;; Housing width (X) at top edge, front face
      {:from       [(- hw) hh hd]
       :to         [hw     hh hd]
       :offset-dir [0 1 0]
       :offset-dist 15
       :label      (str housing-width "mm")}
      ;; Housing height (Y) at right side
      {:from       [hw (- hh) 0]
       :to         [hw    hh  0]
       :offset-dir [1 0 0]
       :offset-dist 15
       :label      (str housing-height "mm")}
      ;; Groove height (Y) at right side, front face
      {:from       [hw (- gh) hd]
       :to         [hw    gh  hd]
       :offset-dir [1 0 0]
       :offset-dist 22
       :label      (str groove-height "mm")}
      ;; Groove depth (Z) — from groove back to front face
      {:from       [hw 0 (- hd groove-depth)]
       :to         [hw 0 hd]
       :offset-dir [1 0 0]
       :offset-dist 30
       :label      (str groove-depth "mm")}]
     :tail
     [;; Tail body length (X)
      {:from       [hw      gh hd]
       :to         [(+ hw tail-body-length) gh hd]
       :offset-dir [0 1 0]
       :offset-dist 15
       :label      (str tail-body-length "mm")}
      ;; Housing depth = full tail Z depth, at right end of body
      {:from       [(+ hw tail-body-length) (- gh) (- hd)]
       :to         [(+ hw tail-body-length) (- gh) hd]
       :offset-dir [0 -1 0]
       :offset-dist 15
       :label      (str housing-depth "mm")}
      ;; Tongue span (X) = housing-width
      {:from       [(- hw) 0 (- hd (/ groove-depth 2))]
       :to         [hw     0 (- hd (/ groove-depth 2))]
       :offset-dir [0 -1 0]
       :offset-dist 20
       :label      (str housing-width "mm")}]}))

(def definition
  {:id      :sliding-dovetail
   :label   "Sliding Dovetail"
   :doc     "Tail board tongue slides along a groove in the housing. Resists pull-out without glue."
   :image   "images/joints/sliding-dovetail.jpg"
   :tools   ["Marking gauge" "Dovetail saw" "Router plane" "Shoulder plane" "Bench chisel"]
   :params  default-params
   :derived-fn (fn [{:keys [groove-height groove-depth housing-depth]}]
                 [["Groove height" (str groove-height "\u00a0mm")]
                  ["Groove depth"  (str groove-depth "\u00a0mm")]
                  ["Shoulder (Z)"  (str (- housing-depth groove-depth) "\u00a0mm")]])
   :dims-fn  build-dims
   :parts   [{:id :housing :label "Housing"     :explode-dir [-1 0 0]}
             {:id :tail    :label "Tail / Shelf" :explode-dir [ 1 0 0]}]
   :build-fn (fn [params]
               {:housing (housing params)
                :tail    (tail    params)})
   :cut-seq [{:step 1 :label "Mark groove centre on housing" :part :housing}
             {:step 2 :label "Rout / chisel groove"          :part :housing}
             {:step 3 :label "Mark tongue width on shelf"    :part :tail}
             {:step 4 :label "Saw tongue cheeks"             :part :tail}
             {:step 5 :label "Plane tongue to sliding fit"   :part :tail}
             {:step 6 :label "Slide in and glue"             :part nil}]})
