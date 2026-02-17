(ns jor.scene.three
  (:require ["three" :as THREE]
            ["three/addons/controls/OrbitControls.js" :refer [OrbitControls]]
            [jor.geometry.explode :as explode]))

;; Single scene state atom — holds all Three.js objects.
;; Never put this in re-frame; Three.js objects are not serialisable.
(defonce ^:private state
  (atom {:renderer nil
         :scene    nil
         :camera   nil
         :controls nil
         :raf-id   nil
         :groups   {}}))

;; ── Init ────────────────────────────────────────────────────────────────────

(defn init!
  "Creates renderer, scene, camera and lights attached to canvas el.
   Must be called once after the <canvas> DOM element is mounted."
  [canvas]
  (let [w        (.-clientWidth canvas)
        h        (.-clientHeight canvas)
        renderer (THREE/WebGLRenderer. #js {:canvas canvas :antialias true
                                            :logarithmicDepthBuffer true})
        scene    (THREE/Scene.)
        camera   (THREE/PerspectiveCamera. 45 (/ w h) 1 1000)
        controls (OrbitControls. camera (.-domElement renderer))]

    (.setSize renderer w h)
    (.setPixelRatio renderer js/window.devicePixelRatio)
    (set! (.-background scene) (THREE/Color. 0x1e1e2e))

    (.set (.-position camera) 200 150 200)
    (.lookAt camera 0 0 0)

    (set! (.-enableDamping controls) true)
    (set! (.-dampingFactor controls) 0.05)

    ;; Lights
    (let [ambient  (THREE/AmbientLight. 0xffffff 0.6)
          dir-l    (THREE/DirectionalLight. 0xffffff 0.9)]
      (.set (.-position dir-l) 150 300 150)
      (.add scene ambient)
      (.add scene dir-l))

    ;; Subtle grid helper for spatial reference
    (let [grid (THREE/GridHelper. 400 20 0x3a3a5a 0x2a2a3e)]
      (.set (.-position grid) 0 -15 0)
      (.add scene grid))

    (swap! state merge {:renderer renderer
                        :scene    scene
                        :camera   camera
                        :controls controls})))

;; ── Resize ──────────────────────────────────────────────────────────────────

(defn resize!
  "Update renderer and camera when canvas container size changes."
  [w h]
  (let [{:keys [^js renderer ^js camera]} @state]
    (when renderer
      (set! (.-aspect camera) (/ w h))
      (.updateProjectionMatrix camera)
      (.setSize renderer w h))))

;; ── Joint rendering ─────────────────────────────────────────────────────────

(defn- clear-joint-groups! []
  (let [{:keys [^js scene groups]} @state]
    (doseq [^js g (vals groups)]
      (.remove scene g)
      ;; Dispose child mesh geometries to avoid GPU memory leaks
      (.traverse g (fn [^js obj]
                     (when (instance? THREE/Mesh obj)
                       (.dispose (.-geometry obj))))))))

(defn rebuild-joint!
  "Rebuild the scene from joint-def + params + explode-factor.
   Called from the React component on every relevant state change."
  [joint-def params explode-factor]
  (let [{:keys [scene]} @state
        _              (clear-joint-groups!)
        part-groups    ((:build-fn joint-def) params)
        parts          (:parts joint-def)
        ex-scale       80  ;; mm separation at explode-factor = 1.0
        ;; Each joint declares :min-explode — the minimum factor needed to keep
        ;; interlocking geometry from Z-fighting (computed per-joint from protrusion depth).
        ex-factor      (max explode-factor (get joint-def :min-explode 0))
        ex-offsets     (explode/offsets parts ex-factor ex-scale)]
    (doseq [{:keys [id]} parts]
      (when-let [grp (get part-groups id)]
        (let [[ox oy oz] (get ex-offsets id [0 0 0])]
          (.set (.-position grp) ox oy oz)
          (.add scene grp))))
    (swap! state assoc :groups part-groups)))

;; ── Render loop ──────────────────────────────────────────────────────────────

(defn- render-frame! []
  (let [{:keys [renderer scene camera controls]} @state]
    (when renderer
      (.update controls)
      (.render renderer scene camera))))

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
      (.lookAt camera 0 0 0)
      (.update controls))))
