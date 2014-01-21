(ns shannon.compressor
  (:require [shannon.io :refer [target finalize! bit-source]]
            [shannon.core :refer [input-stateful-coder output-stateful-coder]]
            [shannon.coding-primitives :refer [encode decode]]
            [shannon.polymorphic-coders :refer [default-coder]]
            [shannon.type-registration]))

(defn- with-output [f]
  (#+clj with-open #+cljs let [sc (output-stateful-coder)]
    (f sc)
    #+clj (.toByteArray (target (finalize! sc)))
    #+cljs (target (finalize! sc))))

(defn- with-input [f source]
  (#+clj with-open #+cljs let [sc (input-stateful-coder (bit-source source))]
         (f sc)))

(defn compress
  ([o] (with-output #(encode o %)))
  ([o coder] (with-output #(encode coder % o))))

(defn decompress
  ([source] (with-input #(decode default-coder %) source))
  ([source coder] (with-input #(decode coder %) source)))
