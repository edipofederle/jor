(ns jor.joints.bridle
  (:require [jor.geometry.primitives :as p]
            [jor.scene.materials :as m]))

;; Bridle joint: post runs vertically (Y axis), slot opens upward at the post top.
;; Rail runs horizontally (Z axis), tongue hangs down into the slot,
;; shoulders rest on top of the cheeks.
;;
;;   ┌─────────────────────────┐   ← rail body (full width, Y = fork-depth + rail-depth)
;;   │    │             │    │
;;   │ sh │   tongue    │ sh │   ← tongue in slot, shoulders on cheeks
;;   └────┘             └────┘
;;      ██               ██       ← post cheeks  (y = 0 to fork-depth)
;;      ████████████████████      ← post body    (y = -body-length to 0)

(def default-params
  {:post-width       50    ; X width of post (and rail body)
   :post-depth       50    ; Z depth of post cross-section; also = fork depth in Y
   :post-length      200   ; total Y height of post (body + fork)
   :rail-depth       20    ; Y thickness of the rail board
   :rail-length      150   ; Z total length of rail (centred at joint)
   :cheek-thickness  12})  ; X thickness of each fork cheek

(defn- post
  [{:keys [post-width post-depth post-length cheek-thickness]}]
  (let [mat         (m/get-mat :wood-light)
        grp         (p/group)
        fork-depth  post-depth                        ; slot depth = post Z depth
        body-length (- post-length fork-depth)        ; 200 - 50 = 150 mm
        cheek-cx    (- (/ post-width 2) (/ cheek-thickness 2))
        ;; Main body — below the fork, running down in Y
        body        (p/set-pos! (p/make-box-mesh post-width body-length post-depth mat)
                                0 (- (/ body-length 2)) 0)
        ;; Two cheeks forming the U-slot (y = 0 → fork-depth)
        lc          (p/set-pos! (p/make-box-mesh cheek-thickness fork-depth post-depth mat)
                                (- cheek-cx) (/ fork-depth 2) 0)
        rc          (p/set-pos! (p/make-box-mesh cheek-thickness fork-depth post-depth mat)
                                cheek-cx (/ fork-depth 2) 0)]
    (p/add-to! grp body lc rc)
    grp))

(defn- rail
  [{:keys [post-width post-depth rail-depth rail-length cheek-thickness]}]
  (let [mat          (m/get-mat :wood-dark)
        grp          (p/group)
        fork-depth   post-depth
        tongue-width (- post-width (* 2 cheek-thickness))  ; 50 - 24 = 26 mm
        ;; Full-width body — sits on top of the post cheeks (y = fork-depth upward)
        body         (p/set-pos! (p/make-box-mesh post-width rail-depth rail-length mat)
                                 0 (+ fork-depth (/ rail-depth 2)) 0)
        ;; Tongue — narrow, goes DOWN into the slot; Z extent = post depth (slot width)
        tongue       (p/set-pos! (p/make-box-mesh tongue-width fork-depth post-depth mat)
                                 0 (/ fork-depth 2) 0)]
    (p/add-to! grp body tongue)
    grp))

(def definition
  {:id      :bridle
   :label   "Bridle"
   :doc     "Open mortise — rail tongue is captured by the post fork. Strong in bending."
   :image   "images/joints/bridle.jpg"
   :params  default-params
   :derived-fn (fn [{:keys [post-width post-depth cheek-thickness]}]
                 (let [tongue (- post-width (* 2 cheek-thickness))]
                   [["Tongue width"  (str tongue "\u00a0mm")]
                    ["Fork depth"    (str post-depth "\u00a0mm")]
                    ["Cheek (each)"  (str cheek-thickness "\u00a0mm")]]))
   :min-explode 0.05  ; tiny gap to eliminate Z-fighting at y=0 and y=fork-depth
   :parts   [{:id :post :label "Post (Fork)"   :explode-dir [0 -1 0]}
             {:id :rail :label "Rail (Tongue)" :explode-dir [0  1 0]}]
   :build-fn (fn [params]
               {:post (post params)
                :rail (rail params)})
   :cut-seq [{:step 1 :label "Mark fork depth on post"       :part :post}
             {:step 2 :label "Saw fork cheeks"               :part :post}
             {:step 3 :label "Chop fork bottom"              :part :post}
             {:step 4 :label "Mark tongue on rail"           :part :rail}
             {:step 5 :label "Saw tongue cheeks + shoulders" :part :rail}
             {:step 6 :label "Fit and glue"                  :part nil}]})
