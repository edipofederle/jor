(ns jor.joints.mortise-tenon
  (:require [jor.geometry.primitives :as p]
            [jor.scene.materials :as m]))

;; Mortise & Tenon:
;;
;;   stile (Y axis, vertical)        rail (Z axis, horizontal)
;;
;;   ┌──────────┐                    ┌──────────────────────────┐
;;   │          │                    │                          │
;;   │  ┌────┐  │ ← mortise (Y×X)   │          ┌────────┐      │
;;   │  │    │  │←stile-depth (Z)→  │          │ tenon  │      │
;;   │  └────┘  │                   │          └────────┘      │
;;   │          │                   └──────────────────────────┘
;;   └──────────┘
;;
;; Tenon enters the stile's Z+ face; mortise goes through full Z depth (through-mortise).
;; Explode separates them along Z.

(def default-params
  {:rail-width        50   ; X width of rail board
   :rail-depth        20   ; Y thickness of rail board
   :rail-length       150  ; Z body length of rail (not including tenon)
   :stile-width       50   ; X width of stile
   :stile-depth       50   ; Z depth of stile (tenon passes through this direction)
   :stile-length      200  ; Y total height of stile
   :tenon-length      50   ; Z length of tenon  (= stile-depth = through-mortise)
   :tenon-width       30   ; X width of tenon   (< stile-width, creates shoulders)
   :tenon-thickness   12}) ; Y height of tenon  (< rail-depth,  creates shoulders)

(defn- stile
  [{:keys [stile-width stile-depth stile-length tenon-width tenon-thickness]}]
  (let [mat  (m/get-mat :wood-dark)
        grp  (p/group)
        hw   (/ stile-width 2)
        hz   (/ stile-depth 2)
        hl   (/ stile-length 2)
        th   (/ tenon-thickness 2)   ; half tenon height (Y)
        tw   (/ tenon-width 2)       ; half tenon width  (X)
        ;; Mortise is centred at y=0.  Stile is built from 4 boxes around the hole.
        ;; ① Bottom slab (below mortise)
        bl   (- hl th)
        bot  (p/set-pos! (p/make-box-mesh stile-width bl stile-depth mat)
                         0 (- (+ th (/ bl 2))) 0)
        ;; ② Top slab (above mortise)
        top  (p/set-pos! (p/make-box-mesh stile-width bl stile-depth mat)
                         0 (+ th (/ bl 2)) 0)
        ;; ③ Left cheek (in X, beside mortise)
        lw   (- hw tw)
        lc   (p/set-pos! (p/make-box-mesh lw tenon-thickness stile-depth mat)
                         (- (+ tw (/ lw 2))) 0 0)
        ;; ④ Right cheek (in X, beside mortise)
        rc   (p/set-pos! (p/make-box-mesh lw tenon-thickness stile-depth mat)
                         (+ tw (/ lw 2)) 0 0)]
    (p/add-to! grp bot top lc rc)
    grp))

(defn- rail
  [{:keys [rail-width rail-depth rail-length stile-depth
           tenon-width tenon-thickness tenon-length]}]
  (let [mat    (m/get-mat :wood-light)
        grp    (p/group)
        hz     (/ stile-depth 2)
        ;; Body — sits in +Z from the stile face
        body   (p/set-pos! (p/make-box-mesh rail-width rail-depth rail-length mat)
                           0 0 (+ hz (/ rail-length 2)))
        ;; Tenon — protrudes from the body in −Z into the mortise.
        ;; Centred so its +Z end is flush with the stile +Z face.
        tenon  (p/set-pos! (p/make-box-mesh tenon-width tenon-thickness tenon-length mat)
                           0 0 (- hz (/ tenon-length 2)))]
    (p/add-to! grp body tenon)
    grp))

(defn- build-dims
  "Dimension annotations split by part so each group moves with its part on explode.
   Returns {:part-id [dim-spec ...] ...} — coords are in each part group's local space
   (which equals world space when assembled, since both groups originate at world origin)."
  [{:keys [stile-width stile-depth stile-length
           rail-width rail-depth rail-length
           tenon-width tenon-length tenon-thickness]}]
  (let [hw (/ stile-width 2)
        hz (/ stile-depth 2)
        hl (/ stile-length 2)
        th (/ tenon-thickness 2)
        tw (/ tenon-width 2)
        rw (/ rail-width 2)
        rd (/ rail-depth 2)]
    {:stile
     [;; Stile height — right side, along Y
      {:from        [hw (- hl) 0]
       :to          [hw    hl  0]
       :offset-dir  [1 0 0]
       :offset-dist 22
       :label       (str stile-length "mm")}
      ;; Stile width — top edge, along X (front face)
      {:from        [(- hw) hl hz]
       :to          [hw     hl hz]
       :offset-dir  [0 1 0]
       :offset-dist 15
       :label       (str stile-width "mm")}]
     :rail
     [;; Tenon depth — below the tenon, along Z (shows mortise depth)
      {:from        [0 (- th) (- hz)]
       :to          [0 (- th)    hz]
       :offset-dir  [0 -1 0]
       :offset-dist 18
       :label       (str tenon-length "mm")}
      ;; Rail body length — above the body, along Z
      {:from        [0 rd hz]
       :to          [0 rd (+ hz rail-length)]
       :offset-dir  [0 1 0]
       :offset-dist 18
       :label       (str rail-length "mm")}
      ;; Rail board thickness — right side of body, along Y
      {:from        [rw (- rd) (+ hz (/ rail-length 2))]
       :to          [rw    rd  (+ hz (/ rail-length 2))]
       :offset-dir  [1 0 0]
       :offset-dist 15
       :label       (str rail-depth "mm")}
      ;; Tenon thickness — right of tenon, along Y
      {:from        [tw (- th) 0]
       :to          [tw    th  0]
       :offset-dir  [1 0 0]
       :offset-dist 12
       :label       (str tenon-thickness "mm")}]}))

(def definition
  {:id      :mortise-tenon
   :label   "Mortise & Tenon"
   :doc     "The foundational frame joint. Tenon tongue seats into mortise pocket."
   :image   "images/joints/mortise-tenon.jpg"
   :tools   ["Marking gauge" "Mortise chisel" "Bench chisel" "Mallet" "Tenon saw" "Router plane"]
   :params  default-params
   :derived-fn (fn [{:keys [rail-width rail-depth tenon-width tenon-thickness tenon-length]}]
                 (let [sh-x (/ (- rail-width tenon-width) 2)
                       sh-y (/ (- rail-depth tenon-thickness) 2)]
                   [["Mortise opening"  (str tenon-width "\u00d7" tenon-thickness "\u00a0mm")]
                    ["Shoulder (width)" (str sh-x "\u00a0mm each side")]
                    ["Shoulder (depth)" (str sh-y "\u00a0mm each side")]
                    ["Tenon length"     (str tenon-length "\u00a0mm")]]))
   :min-explode 0.05  ; tiny gap to prevent Z-fighting at the stile faces
   :parts   [{:id :rail  :label "Rail (Tenon)"    :explode-dir [0 0  1]}
             {:id :stile :label "Stile (Mortise)" :explode-dir [0 0 -1]}]
   :build-fn (fn [params]
               {:rail  (rail  params)
                :stile (stile params)})
   :dims-fn  build-dims
   :cut-seq [{:step 1 :label "Mark mortise on stile"    :part :stile}
             {:step 2 :label "Chop mortise"             :part :stile}
             {:step 3 :label "Mark tenon on rail"       :part :rail}
             {:step 4 :label "Saw tenon cheeks"         :part :rail}
             {:step 5 :label "Saw tenon shoulders"      :part :rail}
             {:step 6 :label "Fit, glue and wedge"      :part nil}]})
