(ns jor.joints.dovetail
  (:require [jor.geometry.primitives :as p]
            [jor.scene.materials :as m]))

;; All dimensions in mm.
;; Tails are rectangular for v0.1 (trapezoidal in v0.2 with CSG).
(def default-params
  {:board-width  80
   :board-depth  20
   :board-length 120
   :tail-count   3
   :tail-width   14})

(defn- tail-board
  [{:keys [board-width board-depth board-length tail-count tail-width]}]
  (let [mat      (m/get-mat :wood-light)
        body-len (- board-length 20)
        grp      (p/group)
        ;; main body behind the joint face
        body     (p/set-pos! (p/make-box-mesh board-width board-depth body-len mat)
                             0 0 (- (/ body-len 2)))
        ;; finger spacing across board width
        spacing  (/ board-width (inc tail-count))]
    (p/add-to! grp body)
    (dotimes [i tail-count]
      (let [x    (- (* (inc i) spacing) (/ board-width 2))
            tail (p/set-pos! (p/make-box-mesh tail-width board-depth 20 mat)
                             x 0 10)]
        (.add grp tail)))
    grp))

(defn- pin-board
  [{:keys [board-width board-depth board-length tail-count]}]
  (let [mat      (m/get-mat :wood-dark)
        body-len (- board-length 20)
        grp      (p/group)
        body     (p/set-pos! (p/make-box-mesh board-width board-depth body-len mat)
                             0 0 (/ body-len 2))
        ;; pins are the negative space between tails → same count + 1 edge pieces
        pin-count (inc tail-count)
        spacing   (/ board-width (inc tail-count))
        pin-w     (* spacing 0.35)]
    (p/add-to! grp body)
    (dotimes [i pin-count]
      (let [x   (- (* i spacing) (/ board-width 2) (- (/ spacing 2)))
            pin (p/set-pos! (p/make-box-mesh pin-w board-depth 20 mat)
                            x 0 -10)]
        (.add grp pin)))
    grp))

(defn- build-dims
  [{:keys [board-width board-depth board-length tail-count tail-width]}]
  (let [bw       board-width
        bd       board-depth
        body-len (- board-length 20)
        spacing  (/ bw (inc tail-count))
        pin-w    (* spacing 0.35)
        hbw      (/ bw 2)
        hbd      (/ bd 2)
        htw      (/ tail-width 2)
        hpw      (/ pin-w 2)
        ;; first tail x (spacing-based, centred around 0)
        t0x      (- spacing hbw)
        ;; first pin x (left-edge pin, centred at 0.5*spacing from left)
        p0x      (- (* 0.5 spacing) hbw)
        fmt      #(if (== (js/Math.round %) %)
                    (str (int %) "mm")
                    (str (.toFixed % 1) "mm"))]
    {:tail-board
     [;; Board width at top-back of body (X)
      {:from       [(- hbw) hbd (- (/ body-len 2))]
       :to         [hbw     hbd (- (/ body-len 2))]
       :offset-dir [0 1 0]
       :offset-dist 15
       :label      (str bw "mm")}
      ;; Board depth at right side of body (Y)
      {:from       [hbw (- hbd) (- (/ body-len 2))]
       :to         [hbw    hbd  (- (/ body-len 2))]
       :offset-dir [1 0 0]
       :offset-dist 15
       :label      (str bd "mm")}
      ;; Tail width at top of first tail (X)
      {:from       [(- t0x htw) hbd 10]
       :to         [(+ t0x htw) hbd 10]
       :offset-dir [0 1 0]
       :offset-dist 20
       :label      (fmt tail-width)}
      ;; Tail projection depth (Z) — 20 mm for all tails
      {:from       [hbw 0 0]
       :to         [hbw 0 20]
       :offset-dir [1 0 0]
       :offset-dist 15
       :label      "20mm"}]
     :pin-board
     [;; Board width at top-back of body (X)
      {:from       [(- hbw) hbd (/ body-len 2)]
       :to         [hbw     hbd (/ body-len 2)]
       :offset-dir [0 1 0]
       :offset-dist 15
       :label      (str bw "mm")}
      ;; Pin width at top of first (leftmost) pin (X)
      {:from       [(- p0x hpw) hbd -10]
       :to         [(+ p0x hpw) hbd -10]
       :offset-dir [0 1 0]
       :offset-dist 20
       :label      (fmt pin-w)}
      ;; Pin projection depth (Z) — 20 mm for all pins
      {:from       [(- hbw) 0 0]
       :to         [(- hbw) 0 -20]
       :offset-dir [-1 0 0]
       :offset-dist 15
       :label      "20mm"}]}))

(def definition
  {:id      :dovetail
   :label   "Dovetail"
   :doc     "Interlocking tails resist pull-apart forces. Classic drawer joint."
   :image   "images/joints/dovetail.jpg"
   :tools   ["Marking gauge" "Sliding bevel" "Dovetail saw" "Coping saw" "Bench chisel" "Mallet"]
   :params      default-params
   :derived-fn (fn [{:keys [board-width tail-count tail-width]}]
                 (let [spacing (/ board-width (inc tail-count))
                       pin-w   (* spacing 0.35)
                       fmt     #(if (== % (js/Math.floor %))
                                  (str (int %) "\u00a0mm")
                                  (str (.toFixed % 1) "\u00a0mm"))]
                   [["Tail spacing (c/c)" (fmt spacing)]
                    ["Pin width (approx)" (fmt pin-w)]
                    ["Tail width"         (fmt tail-width)]]))
   :dims-fn  build-dims
   :min-explode 0.15  ; tails are 20 mm; need f≥0.125 to avoid interpenetration
   :parts   [{:id :tail-board :label "Tail Board" :explode-dir [0 0 -1]}
             {:id :pin-board  :label "Pin Board"  :explode-dir [0 0  1]}]
   :build-fn (fn [params]
               {:tail-board (tail-board params)
                :pin-board  (pin-board  params)})
   :cut-seq [{:step 1 :label "Mark tail layout"      :part :tail-board}
             {:step 2 :label "Saw tail cheeks"        :part :tail-board}
             {:step 3 :label "Chop tail baselines"    :part :tail-board}
             {:step 4 :label "Transfer to pin board"  :part :pin-board}
             {:step 5 :label "Saw pin cheeks"         :part :pin-board}
             {:step 6 :label "Chop pin baselines"     :part :pin-board}
             {:step 7 :label "Test fit"               :part nil}]})
