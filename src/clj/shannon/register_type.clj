(ns shannon.register-type
  (:require [shannon.coding-primitives :refer [Codeable encode decode custom]]
            [shannon.polymorphic-coders :refer [register-type! default-coder]]))

(defmacro register-polymorphic-type! [k ptype & {:keys [coder test pr]}]
  (let [o (gensym "o")
        s (gensym "s")
        types (if (coll? ptype) ptype [ptype])]
    `(do (register-type! default-coder ~k
                         :pr ~pr :coder ~coder :test ~test)
         ~@(map
            (fn [t]
              `(extend-protocol Codeable
                 ~t
                 (~'encode [~o ~s] (encode default-coder ~s ~o ~k))
                 (~'decode [~o ~s] (decode default-coder ~s))))
            types))))
