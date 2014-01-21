(ns shannon.math-macros
  (:require [clojure.math.numeric-tower :refer [expt]]))

(defmacro pow2 [e]
  (double (expt 2 e)))
