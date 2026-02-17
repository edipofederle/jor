(ns jor.geometry.explode)

(defn offsets
  "Given a sequence of part-defs (each with :id and :explode-dir)
   and a factor in [0..1], returns a map of part-id → [ox oy oz].
   explode-scale is the max mm separation at factor=1.0."
  [parts factor explode-scale]
  (reduce
   (fn [acc {:keys [id explode-dir]}]
     (let [[dx dy dz] explode-dir
           d (* factor explode-scale)]
       (assoc acc id [(* dx d) (* dy d) (* dz d)])))
   {}
   parts))
