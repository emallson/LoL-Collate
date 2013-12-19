(ns atlanis.utils)

(defmacro unless
  "Convenience macro. Evaluates `body` only when `condition` is false."
  [condition & body]
  `(when (not ~condition)
     ~@body))

(defn now
  []
  (.getTime (java.util.Date.)))
