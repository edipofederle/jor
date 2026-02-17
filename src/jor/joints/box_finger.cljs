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

(def definition
  {:id      :box-finger
   :label   "Box / Finger"
   :doc     "Uniform rectangular fingers maximise glue surface. Ideal for boxes."
   :params      default-params
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
