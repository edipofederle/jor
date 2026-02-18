(ns jor.joints.half-lap
  (:require [jor.geometry.primitives :as p]
            [jor.scene.materials :as m]))

(def default-params
  {:member-width   60
   :member-depth   20
   :member-length  150
   :overlap-length 50})

(defn- member-a
  [{:keys [member-width member-depth member-length overlap-length]}]
  (let [mat     (m/get-mat :wood-light)
        grp     (p/group)
        half-d  (/ member-depth 2)
        body-l  (- member-length overlap-length)
        ;; main body
        body    (p/set-pos! (p/make-box-mesh member-width member-depth body-l mat)
                            0 0 (- (/ body-l 2)))
        ;; overlap section — only upper half remains (bottom half removed)
        overlap (p/set-pos! (p/make-box-mesh member-width half-d overlap-length mat)
                            0 (/ half-d 2) (/ overlap-length 2))]
    (p/add-to! grp body overlap)
    grp))

(defn- member-b
  [{:keys [member-width member-depth member-length overlap-length]}]
  (let [mat     (m/get-mat :wood-dark)
        grp     (p/group)
        half-d  (/ member-depth 2)
        body-l  (- member-length overlap-length)
        ;; main body — runs in opposite direction
        body    (p/set-pos! (p/make-box-mesh member-width member-depth body-l mat)
                            0 0 (/ body-l 2))
        ;; overlap section — only lower half remains (top half removed)
        overlap (p/set-pos! (p/make-box-mesh member-width half-d overlap-length mat)
                            0 (- (/ half-d 2)) (- (/ overlap-length 2)))]
    (p/add-to! grp body overlap)
    grp))

(defn- build-dims
  [{:keys [member-width member-depth member-length overlap-length]}]
  (let [mw     member-width
        md     member-depth
        ol     overlap-length
        body-l (- member-length ol)
        hmw    (/ mw 2)
        hmd    (/ md 2)
        hd     (/ md 2)]   ; cut depth = half the member depth
    {:member-a
     [;; Width across top of body
      {:from       [(- hmw) hmd (- (/ body-l 2))]
       :to         [hmw     hmd (- (/ body-l 2))]
       :offset-dir [0 1 0]
       :offset-dist 15
       :label      (str mw "mm")}
      ;; Full depth at right side of body
      {:from       [hmw (- hmd) (- (/ body-l 2))]
       :to         [hmw    hmd  (- (/ body-l 2))]
       :offset-dir [1 0 0]
       :offset-dist 15
       :label      (str md "mm")}
      ;; Cut depth at the overlap zone (y=0 to y=half-depth)
      {:from       [hmw 0 (/ ol 2)]
       :to         [hmw hd (/ ol 2)]
       :offset-dir [1 0 0]
       :offset-dist 22
       :label      (str hd "mm")}
      ;; Overlap / cut length along Z
      {:from       [0 hd 0]
       :to         [0 hd ol]
       :offset-dir [0 1 0]
       :offset-dist 15
       :label      (str ol "mm")}]
     :member-b
     [;; Width across top of body
      {:from       [(- hmw) hmd (/ body-l 2)]
       :to         [hmw     hmd (/ body-l 2)]
       :offset-dir [0 1 0]
       :offset-dist 15
       :label      (str mw "mm")}
      ;; Cut depth at the overlap zone (y=0 down to y=-half-depth)
      {:from       [(- hmw) 0 (- (/ ol 2))]
       :to         [(- hmw) (- hd) (- (/ ol 2))]
       :offset-dir [-1 0 0]
       :offset-dist 22
       :label      (str hd "mm")}
      ;; Overlap length (goes in -Z for member-b)
      {:from       [0 (- hd) 0]
       :to         [0 (- hd) (- ol)]
       :offset-dir [0 -1 0]
       :offset-dist 15
       :label      (str ol "mm")}]}))

(def definition
  {:id      :half-lap
   :label   "Half-Lap"
   :doc     "Each member loses half its depth at the joint. Combined they stay flush."
   :image   "images/joints/half-lap.jpg"
   :tools   ["Marking gauge" "Tenon saw" "Bench chisel" "Mallet" "Router plane"]
   :params      default-params
   :derived-fn (fn [{:keys [member-depth overlap-length]}]
                 [["Cut depth"   (str (/ member-depth 2) "\u00a0mm")]
                  ["Cut length"  (str overlap-length "\u00a0mm")]])
   :dims-fn  build-dims
   :min-explode 0.10  ; half-depth overlap is 10 mm; need f≥0.0625 to avoid interpenetration
   :parts   [{:id :member-a :label "Member A" :explode-dir [0  1 0]}
             {:id :member-b :label "Member B" :explode-dir [0 -1 0]}]
   :build-fn (fn [params]
               {:member-a (member-a params)
                :member-b (member-b params)})
   :cut-seq [{:step 1 :label "Mark half-depth on A"   :part :member-a}
             {:step 2 :label "Saw cheeks — Member A"  :part :member-a}
             {:step 3 :label "Chisel waste — Member A" :part :member-a}
             {:step 4 :label "Repeat for Member B"    :part :member-b}
             {:step 5 :label "Glue and clamp"         :part nil}]})
