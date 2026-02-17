(ns jor.db)

(def default-db
  {;; active joint keyword — matches a key in joints/registry
   :active-joint-id  :dovetail

   ;; sparse param overrides per joint: {joint-id {param-key value}}
   ;; merged over each joint's :params defaults at subscription layer
   :joint-params {}

   :viewport
   {:explode-factor 0.0}

   ;; Phase 3 — cut sequence animation
   :animation
   {:playing? false
    :cut-step 0}

   :ui
   {:sidebar-open? true}})
