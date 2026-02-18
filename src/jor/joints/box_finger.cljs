(ns jor.joints.box-finger
  (:require [jor.geometry.primitives :as p]
            [jor.scene.materials :as m]))

(def default-params
  {:board-width   80
   :board-depth   15
   :board-length  120
   :finger-count  4
   :finger-width  10})

(defn- board-a
  [{:keys [board-width board-depth board-length finger-count finger-width]}]
  (let [mat      (m/get-mat :wood-light)
        body-len (- board-length finger-width)
        grp      (p/group)
        body     (p/set-pos! (p/make-box-mesh board-width board-depth body-len mat)
                             0 0 (- (/ body-len 2)))]
    (p/add-to! grp body)
    ;; fingers on even positions
    (let [total-w  (* finger-count finger-width 2)
          offset-x (/ (- board-width total-w) 2)]
      (dotimes [i finger-count]
        (let [x    (+ offset-x (* i finger-width 2) (/ finger-width 2) (- (/ board-width 2)))
              fing (p/set-pos! (p/make-box-mesh finger-width board-depth finger-width mat)
                               x 0 (/ finger-width 2))]
          (.add grp fing))))
    grp))

(defn- board-b
  [{:keys [board-width board-depth board-length finger-count finger-width]}]
  (let [mat      (m/get-mat :wood-dark)
        body-len (- board-length finger-width)
        grp      (p/group)
        body     (p/set-pos! (p/make-box-mesh board-width board-depth body-len mat)
                             0 0 (/ body-len 2))]
    (p/add-to! grp body)
    ;; fingers on odd positions (offset by one finger-width from board-a)
    (let [total-w  (* finger-count finger-width 2)
          offset-x (/ (- board-width total-w) 2)]
      (dotimes [i finger-count]
        (let [x    (+ offset-x (* i finger-width 2) (* 1.5 finger-width) (- (/ board-width 2)))
              fing (p/set-pos! (p/make-box-mesh finger-width board-depth finger-width mat)
                               x 0 (- (/ finger-width 2)))]
          (.add grp fing))))
    grp))

(defn- build-dims
  [{:keys [board-width board-depth board-length finger-count finger-width]}]
  (let [bw       board-width
        bd       board-depth
        fw       finger-width
        body-len (- board-length fw)
        hbw      (/ bw 2)
        hbd      (/ bd 2)
        total-w  (* finger-count fw 2)
        ;; x centre of board-a's first finger
        f0x      (+ (/ (- bw total-w) 2) (/ fw 2) (- hbw))]
    {:board-a
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
      ;; Finger width on top of first finger (Z)
      {:from       [f0x hbd 0]
       :to         [f0x hbd fw]
       :offset-dir [0 1 0]
       :offset-dist 15
       :label      (str fw "mm")}
      ;; Body length (Z)
      {:from       [hbw hbd 0]
       :to         [hbw hbd (- body-len)]
       :offset-dir [1 0 0]
       :offset-dist 15
       :label      (str body-len "mm")}]
     :board-b
     [;; Board width at top-back of body (X)
      {:from       [(- hbw) hbd (/ body-len 2)]
       :to         [hbw     hbd (/ body-len 2)]
       :offset-dir [0 1 0]
       :offset-dist 15
       :label      (str bw "mm")}
      ;; Board depth at right side of body (Y)
      {:from       [hbw (- hbd) (/ body-len 2)]
       :to         [hbw    hbd  (/ body-len 2)]
       :offset-dir [1 0 0]
       :offset-dist 15
       :label      (str bd "mm")}
      ;; Body length (Z)
      {:from       [hbw hbd 0]
       :to         [hbw hbd body-len]
       :offset-dir [1 0 0]
       :offset-dist 15
       :label      (str body-len "mm")}]}))

(def definition
  {:id      :box-finger
   :label   "Box / Finger"
   :doc     "Uniform rectangular fingers maximise glue surface. Ideal for boxes."
   :image   "images/joints/box-finger.jpg"
   :params      default-params
   :derived-fn (fn [{:keys [board-width finger-count finger-width]}]
                 (let [total-w (* finger-count finger-width 2)
                       edge    (/ (- board-width total-w) 2)]
                   [["Finger pitch"  (str (* finger-width 2) "\u00a0mm")]
                    ["Edge offset"   (str edge "\u00a0mm")]]))
   :dims-fn  build-dims
   :min-explode 0.10  ; fingers are 10 mm; need f≥0.0625 to avoid interpenetration
   :parts   [{:id :board-a :label "Board A" :explode-dir [0 0 -1]}
             {:id :board-b :label "Board B" :explode-dir [0 0  1]}]
   :build-fn (fn [params]
               {:board-a (board-a params)
                :board-b (board-b params)})
   :cut-seq [{:step 1 :label "Set stop block to finger-width" :part :board-a}
             {:step 2 :label "Cut fingers — Board A"          :part :board-a}
             {:step 3 :label "Index and cut — Board B"        :part :board-b}
             {:step 4 :label "Test fit and glue"              :part nil}]})
