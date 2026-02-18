(ns jor.db)

(def default-db
  {;; active joint keyword — matches a key in joints/registry
   :active-joint-id  :dovetail

   ;; sparse param overrides per joint: {joint-id {param-key value}}
   ;; merged over each joint's :params defaults at subscription layer
   :joint-params {}

   :viewport
   {:explode-factor 0.0}

   ;; cut-step — the currently active {:step n :label "..." :part kw} or nil
   :animation
   {:playing? false
    :cut-step nil}

   :ui
   {:sidebar-open? true}})
