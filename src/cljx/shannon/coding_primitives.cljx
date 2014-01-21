(ns shannon.coding-primitives
  (:require [shannon.compatibility :refer [abs]]
            [shannon.core :refer [decode-symbol encode-symbol]]
            [shannon.distributions :refer [benford-distribution
                                           constant-distribution
                                           custom-distribution
                                           geometric-distribution
                                           uniform-distribution
                                           zipf-distribution]]))

(defprotocol Codeable
  (encode [o scoder] [o scoder sym] [o scoder sym typehint])
  (decode [o scoder]))

(deftype AtomCoder [distribution]
  Codeable
  (encode [_ scoder sym]
    (encode-symbol scoder distribution sym))
  (decode [_ scoder]
    (decode-symbol scoder distribution)))

(defn uniform [N] (AtomCoder. (uniform-distribution N)))
(defn zipf [N] (AtomCoder. (zipf-distribution N)))
(defn geometric [mu] (AtomCoder. (geometric-distribution mu)))
(defn benford [base digits] (AtomCoder. (benford-distribution base digits)))
(defn custom [options] (AtomCoder. (custom-distribution options)))
(defn constant [value] (AtomCoder. (constant-distribution value)))

(deftype FixedArrayCoder [coders]
  Codeable
  (encode [_ scoder syms]
    (loop [c coders, s syms]
      (when c
        (encode (first c) scoder (first s))
        (recur (next c) (next s)))))
  (decode [_ scoder]
    (map (fn [c] (decode c scoder)) coders)))

(defn fixed-array [coders] (FixedArrayCoder. coders))

(deftype TransformCoder [coder encodefn decodefn]
  Codeable
  (encode [_ scoder sym] (encode coder scoder (encodefn sym)))
  (decode [_ scoder] (decodefn (decode coder scoder))))

(defn transform [coder encodefn decodefn]
  (TransformCoder. coder encodefn decodefn))

(def ^:private signcoder (uniform 2))

(deftype SignCoder [numcoder]
  Codeable
  (encode [_ scoder sym]
    (let [sign (if (neg? sym) 1 0)]
      (encode signcoder scoder sign)
      (encode numcoder scoder (abs sym))))
  (decode [_ scoder]
    (let [sign (decode signcoder scoder)
          num (decode numcoder scoder)]
      (if (pos? sign)
        (- num)
        num))))

(defn signed [coder] (SignCoder. coder))

(deftype VariableArrayCoder [coder countcoder]
  Codeable
  (encode [_ scoder sym]
    (encode countcoder scoder (count sym))
    (doseq [s sym] (encode coder scoder s)))
  (decode [_ scoder]
    (let [cnt (decode countcoder scoder)]
      (doall (repeatedly cnt #(decode coder scoder))))))

(defn variable-array [coder countcoder]
  (VariableArrayCoder. coder countcoder))

(def ^:private presentcoder (custom {true 0.5, false 0.5}))

(deftype SparseArray [maxcnt valuecoder]
  Codeable
  (encode [_ scoder sym]
    (doseq [i (range maxcnt)]
      (if (contains? sym i)
        (do (encode presentcoder scoder true)
            (encode valuecoder scoder (get sym i)))
        (encode presentcoder scoder false))))
  (decode [_ scoder]
    (loop [i 0, sarr {}]
      (if (< i maxcnt)
        (if (decode presentcoder scoder)
          (recur (inc i) (assoc sarr i (decode valuecoder scoder)))
          (recur (inc i) sarr))
        sarr))))

(defn sparse-array [maxcnt valuecoder]
  (SparseArray. maxcnt valuecoder))
