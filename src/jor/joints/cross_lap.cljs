(ns jor.joints.cross-lap
  (:require [jor.geometry.primitives :as p]
            [jor.scene.materials :as m]))

;; Cross-Lap (Cross-Halving) joint:
;; Member A runs along Z, Member B runs along X.
;; Each is notched to half-depth at the crossing — they share the same Y thickness.
;;
;;        ─── Member B (X axis) ───
;;               │  upper half
;;    ───────────┼─────────────────  Member A (Z axis)
;;               │  lower half

(def default-params
  {:member-width    40   ; cross-section width — same for both (X for A, Z for B)
   :member-depth    20   ; Y thickness — same for both
   :member-a-length 150  ; total Z length of Member A
   :member-b-length 150  ; total X length of Member B
   })

(defn- member-a
  [{:keys [member-width member-depth member-a-length]}]
  (let [mat    (m/get-mat :wood-light)
        grp    (p/group)
        hmd    (/ member-depth 2)
        hla    (/ member-a-length 2)
        hmw    (/ member-width 2)
        body-l (- hla hmw)
        ;; Body left of notch: full cross-section, z from -hla to -hmw
        left   (p/set-pos! (p/make-box-mesh member-width member-depth body-l mat)
                           0 0 (- (/ (+ hla hmw) 2)))
        ;; Body right of notch: full cross-section, z from hmw to hla
        right  (p/set-pos! (p/make-box-mesh member-width member-depth body-l mat)
                           0 0 (/ (+ hla hmw) 2))
        ;; Bottom half at crossing — A keeps the lower half
        notch  (p/set-pos! (p/make-box-mesh member-width hmd member-width mat)
                           0 (- (/ hmd 2)) 0)]
    (p/add-to! grp left right notch)
    grp))

(defn- member-b
  [{:keys [member-width member-depth member-b-length]}]
  (let [mat    (m/get-mat :wood-dark)
        grp    (p/group)
        hmd    (/ member-depth 2)
        hlb    (/ member-b-length 2)
        hmw    (/ member-width 2)
        body-l (- hlb hmw)
        ;; Body left of notch: full cross-section, x from -hlb to -hmw
        left   (p/set-pos! (p/make-box-mesh body-l member-depth member-width mat)
                           (- (/ (+ hlb hmw) 2)) 0 0)
        ;; Body right of notch: full cross-section, x from hmw to hlb
        right  (p/set-pos! (p/make-box-mesh body-l member-depth member-width mat)
                           (/ (+ hlb hmw) 2) 0 0)
        ;; Top half at crossing — B keeps the upper half
        notch  (p/set-pos! (p/make-box-mesh member-width hmd member-width mat)
                           0 (/ hmd 2) 0)]
    (p/add-to! grp left right notch)
    grp))

(defn- build-dims
  [{:keys [member-width member-depth member-a-length member-b-length]}]
  (let [mw  member-width
        md  member-depth
        hmd (/ md 2)
        hla (/ member-a-length 2)
        hlb (/ member-b-length 2)
        hmw (/ mw 2)]
    {:member-a
     [;; Width (X) at top of left body
      {:from       [(- hmw) hmd (- (/ (+ hla hmw) 2))]
       :to         [hmw     hmd (- (/ (+ hla hmw) 2))]
       :offset-dir [0 1 0]
       :offset-dist 15
       :label      (str mw "mm")}
      ;; Full depth (Y) at right side of left body
      {:from       [hmw (- hmd) (- (/ (+ hla hmw) 2))]
       :to         [hmw    hmd  (- (/ (+ hla hmw) 2))]
       :offset-dir [1 0 0]
       :offset-dist 15
       :label      (str md "mm")}
      ;; Notch depth (Y) — lower half that remains at crossing
      {:from       [hmw (- hmd) 0]
       :to         [hmw 0       0]
       :offset-dir [1 0 0]
       :offset-dist 22
       :label      (str hmd "mm")}
      ;; Total length (Z)
      {:from       [hmw 0 (- hla)]
       :to         [hmw 0 hla]
       :offset-dir [1 0 0]
       :offset-dist 28
       :label      (str member-a-length "mm")}]
     :member-b
     [;; Width (Z) at top of left body
      {:from       [(- (/ (+ hlb hmw) 2)) hmd (- hmw)]
       :to         [(- (/ (+ hlb hmw) 2)) hmd hmw]
       :offset-dir [0 1 0]
       :offset-dist 15
       :label      (str mw "mm")}
      ;; Notch depth (Y) — upper half that remains at crossing
      {:from       [0 0   (- hmw)]
       :to         [0 hmd (- hmw)]
       :offset-dir [0 0 -1]
       :offset-dist 20
       :label      (str hmd "mm")}
      ;; Total length (X)
      {:from       [(- hlb) hmd 0]
       :to         [hlb     hmd 0]
       :offset-dir [0 1 0]
       :offset-dist 20
       :label      (str member-b-length "mm")}]}))

(def definition
  {:id      :cross-lap
   :label   "Cross-Lap"
   :doc     "Two members cross at mid-span, each notched to half-depth. Simple and strong when glued."
   :image   "images/joints/cross-lap.jpg"
   :params  default-params
   :derived-fn (fn [{:keys [member-depth member-width]}]
                 [["Notch depth" (str (/ member-depth 2) "\u00a0mm")]
                  ["Notch width" (str member-width "\u00a0mm")]])
   :dims-fn  build-dims
   :min-explode 0.10
   :parts   [{:id :member-a :label "Member A" :explode-dir [0  1 0]}
             {:id :member-b :label "Member B" :explode-dir [0 -1 0]}]
   :build-fn (fn [params]
               {:member-a (member-a params)
                :member-b (member-b params)})
   :cut-seq [{:step 1 :label "Mark notch width on A"   :part :member-a}
             {:step 2 :label "Saw notch cheeks — A"    :part :member-a}
             {:step 3 :label "Chisel notch waste — A"  :part :member-a}
             {:step 4 :label "Mark notch on B from A"  :part :member-b}
             {:step 5 :label "Saw notch cheeks — B"    :part :member-b}
             {:step 6 :label "Chisel notch waste — B"  :part :member-b}
             {:step 7 :label "Test fit and glue"       :part nil}]})
