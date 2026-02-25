(ns jor.scene.three
  (:require ["three" :as THREE]
            ["three/addons/controls/OrbitControls.js" :refer [OrbitControls]]
            ["three/addons/renderers/CSS2DRenderer.js" :refer [CSS2DRenderer]]
            [jor.geometry.explode :as explode]
            [jor.geometry.dimensions :as dimensions]))

;; Single scene state atom — holds all Three.js objects.
;; Never put this in re-frame; Three.js objects are not serialisable.
(defonce ^:private state
  (atom {:renderer        nil
         :css2d           nil   ; CSS2DRenderer for HTML labels
         :scene           nil
         :camera          nil
         :controls        nil
         :raf-id          nil
         :groups          {}
         :show-dims?      false
         :current-joint   nil  ; cached for animation / dim toggling
         :current-params  nil
         :highlighted-part nil ; part-id being highlighted, or nil
         :anim            {:playing? false :factor 0.0 :dir 1}}))

;; ── Init ────────────────────────────────────────────────────────────────────

(defn init!
  "Creates renderer, scene, camera and lights attached to canvas el.
   Must be called once after the <canvas> DOM element is mounted."
  [canvas]
  (let [w         (.-clientWidth canvas)
        h         (.-clientHeight canvas)
        renderer  (THREE/WebGLRenderer. #js {:canvas canvas :antialias true
                                             :logarithmicDepthBuffer true})
        css2d     (CSS2DRenderer.)
        container (.-parentElement canvas)
        scene     (THREE/Scene.)
        camera    (THREE/PerspectiveCamera. 45 (/ w h) 1 1000)
        controls  (OrbitControls. camera (.-domElement renderer))]

    (.setSize renderer w h)
    (.setPixelRatio renderer js/window.devicePixelRatio)
    (set! (.-background scene) (THREE/Color. 0x1e1e2e))

    ;; CSS2DRenderer — same pixel size as WebGL canvas, sits on top of it.
    (.setSize css2d w h)
    (let [^js el (.-domElement css2d)]
      (set! (.. el -style -position)      "absolute")
      (set! (.. el -style -top)           "0")
      (set! (.. el -style -left)          "0")
      (set! (.. el -style -pointerEvents) "none"))
    (.appendChild container (.-domElement css2d))

    (.set (.-position camera) 200 150 200)
    (.lookAt camera 0 0 0)

    (set! (.-enableDamping controls) true)
    (set! (.-dampingFactor controls) 0.05)

    ;; Lights
    (let [ambient (THREE/AmbientLight. 0xffffff 0.6)
          dir-l   (THREE/DirectionalLight. 0xffffff 0.9)]
      (.set (.-position dir-l) 150 300 150)
      (.add scene ambient)
      (.add scene dir-l))

    ;; Subtle grid helper for spatial reference
    (let [grid (THREE/GridHelper. 400 20 0x3a3a5a 0x2a2a3e)]
      (.set (.-position grid) 0 -15 0)
      (.add scene grid))

    (swap! state merge {:renderer renderer
                        :css2d    css2d
                        :scene    scene
                        :camera   camera
                        :controls controls
                        :show-dims? false})))

;; ── Resize ──────────────────────────────────────────────────────────────────

(defn resize!
  "Update renderer and camera when canvas container size changes."
  [w h]
  (let [{:keys [^js renderer ^js css2d ^js camera]} @state]
    (when renderer
      (set! (.-aspect camera) (/ w h))
      (.updateProjectionMatrix camera)
      (.setSize renderer w h)
      (when css2d (.setSize css2d w h)))))

;; ── Joint rendering ─────────────────────────────────────────────────────────

(defn- remove-css2d-el! [^js obj]
  (when (.-isCSS2DObject obj)
    (when-let [^js el (.-element obj)]
      (when (.-parentNode el)
        (.removeChild (.-parentNode el) el)))))

(defn- clear-joint-groups! []
  (let [{:keys [^js scene groups]} @state]
    (doseq [^js g (vals groups)]
      (.remove scene g)
      ;; Dispose geometries and scrub CSS2DObject DOM elements from the overlay.
      (.traverse g (fn [^js obj]
                     (when (.-geometry obj)
                       (.dispose (.-geometry obj)))
                     (remove-css2d-el! obj))))))

(defn- apply-explode!
  "Update group positions for the given explode factor without touching geometry."
  [joint-def factor]
  (let [{:keys [groups]} @state
        parts      (:parts joint-def)
        ex-scale   80
        ex-factor  (max factor (get joint-def :min-explode 0))
        ex-offsets (explode/offsets parts ex-factor ex-scale)]
    (doseq [{:keys [id]} parts]
      (when-let [^js grp (get groups id)]
        (let [[ox oy oz] (get ex-offsets id [0 0 0])]
          (.set (.-position grp) ox oy oz))))))

;; ── Dimension annotations ────────────────────────────────────────────────────
;; Dim groups are children of their respective part groups so they move with
;; parts during explode/animate. CSS2DRenderer re-projects labels every frame.

(defn- add-dims-to-groups!
  "Build dim annotations and add each as a tagged child of its part group."
  [joint-def params]
  (when-let [dims-fn (:dims-fn joint-def)]
    (let [groups   (:groups @state)
          by-part  (dims-fn params)]
      (doseq [[part-id specs] by-part]
        (when-let [^js grp (get groups part-id)]
          (let [^js dim-grp (dimensions/build-dims! specs)]
            (set! (.. dim-grp -userData -jorDims) true)
            (.add grp dim-grp)))))))

(defn- remove-dims-from-groups!
  "Remove and dispose all tagged dim child groups from every part group."
  []
  (doseq [^js grp (vals (:groups @state))]
    (when grp
      ;; Collect first to avoid mutating while iterating.
      (let [to-remove (filterv #(.. ^js % -userData -jorDims)
                               (array-seq (.-children grp)))]
        (doseq [^js dg to-remove]
          (.remove grp dg)
          (.traverse dg (fn [^js obj]
                          (when (.-geometry obj)
                            (.dispose (.-geometry obj)))
                          (remove-css2d-el! obj))))))))

(defn toggle-dims!
  "Show or hide dimension annotations for the active joint."
  []
  (swap! state update :show-dims? not)
  (let [{:keys [current-joint current-params show-dims?]} @state]
    (if show-dims?
      (add-dims-to-groups! current-joint current-params)
      (remove-dims-from-groups!))))

(defn dims-showing? []
  (:show-dims? @state))

(defn clear-dims!
  "Turn off dimension annotations unconditionally; no-op if already off."
  []
  (when (:show-dims? @state)
    (remove-dims-from-groups!)
    (swap! state assoc :show-dims? false)))

;; ── Part highlighting ────────────────────────────────────────────────────────

(defn highlight-step!
  "Dim all part groups except part-id. Pass nil to restore everything.
   Clones materials on the way down so shared materials are not mutated."
  [part-id]
  (swap! state assoc :highlighted-part part-id)
  (let [{:keys [groups]} @state]
    (doseq [[gid ^js grp] groups]
      (let [active? (or (nil? part-id) (= gid part-id))]
        (.traverse grp
                   (fn [^js obj]
                     (when (.-isMesh obj)
                       (if active?
                         (when-let [orig (.. obj -userData -origMat)]
                           (set! (.-material obj) orig)
                           (js-delete (.-userData obj) "origMat"))
                         (when-not (.. obj -userData -origMat)
                           (let [orig (.-material obj)
                                 dim  (.clone orig)]
                             (set! (.-transparent dim) true)
                             (set! (.-depthWrite dim) false)
                             (set! (.-opacity dim) 0.15)
                             (set! (.-needsUpdate dim) true)
                             (aset (.-userData obj) "origMat" orig)
                             (set! (.-material obj) dim)))))))))))

;; ── Rebuild joint ────────────────────────────────────────────────────────────

(defn rebuild-joint!
  "Rebuild scene geometry from joint-def + params, then apply explode-factor."
  [joint-def params explode-factor]
  (let [{:keys [scene]} @state
        _           (clear-joint-groups!)
        part-groups ((:build-fn joint-def) params)]
    (swap! state assoc :groups part-groups
                       :current-joint  joint-def
                       :current-params params)
    (apply-explode! joint-def explode-factor)
    (doseq [{:keys [id]} (:parts joint-def)]
      (when-let [^js grp (get part-groups id)]
        (.add scene grp)))
    ;; Re-attach dim annotations if they were visible before the rebuild.
    (when (:show-dims? @state)
      (add-dims-to-groups! joint-def params))
    ;; Re-apply part highlighting if a step was active.
    (when-let [part (:highlighted-part @state)]
      (highlight-step! part))))

;; ── Animation ────────────────────────────────────────────────────────────────

(defn- advance-anim! []
  (let [{:keys [anim current-joint]} @state]
    (when (and (:playing? anim) current-joint)
      (let [f    (:factor anim)
            dir  (:dir anim)
            nf   (+ f (* dir 0.008))
            [nf2 ndir] (cond
                         (>= nf 1.0) [(- 2.0 nf) -1]
                         (<= nf 0.0) [(- nf) 1]
                         :else       [nf dir])]
        (swap! state #(-> %
                          (assoc-in [:anim :factor] nf2)
                          (assoc-in [:anim :dir] ndir)))
        (apply-explode! current-joint nf2)))))

(defn toggle-anim!
  "Start or stop the ping-pong animation. When starting, resets to assembled."
  []
  (let [was-playing? (get-in @state [:anim :playing?])]
    (if was-playing?
      (swap! state assoc-in [:anim :playing?] false)
      (swap! state #(-> %
                        (assoc-in [:anim :playing?] true)
                        (assoc-in [:anim :factor] 0.0)
                        (assoc-in [:anim :dir] 1))))))

(defn anim-playing? []
  (get-in @state [:anim :playing?]))

(defn anim-factor []
  (get-in @state [:anim :factor]))

;; ── Render loop ──────────────────────────────────────────────────────────────

(defn- render-frame! []
  (let [{:keys [renderer css2d scene camera controls]} @state]
    (when renderer
      (advance-anim!)
      (.update controls)
      (.render renderer scene camera)
      (when css2d
        (.render css2d scene camera)))))

(defn start-loop! []
  (letfn [(tick []
            (render-frame!)
            (swap! state assoc :raf-id (js/requestAnimationFrame tick)))]
    (tick)))

(defn stop-loop! []
  (when-let [id (:raf-id @state)]
    (js/cancelAnimationFrame id)
    (swap! state assoc :raf-id nil)))

(defn reset-camera! []
  (let [{:keys [^js camera ^js controls]} @state]
    (when camera
      (.set (.-position camera) 200 150 200)
      (.set (.-target controls) 0 0 0)
      (.update controls))))
