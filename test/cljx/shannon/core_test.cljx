(ns shannon.core-test
  (:require #+clj [clojure.test :refer [deftest testing is]]
            #+cljs [cemerick.cljs.test :as t]
            [shannon.core :refer [decode-symbol
                                  encode-symbol
                                  input-stateful-coder
                                  output-stateful-coder]]
            [shannon.distributions :refer [custom-distribution]]
            [shannon.io :refer [BitCoder  ConsumesBits EmitsBits
                                finalize! target]])
  #+cljs (:require-macros [cemerick.cljs.test :refer [is deftest with-test
                                                      run-tests testing]]))

(deftype BitVectorSource [v]
  EmitsBits
  (read! [_]
    (let [b (first @v)]
      (swap! v rest)
      b))
  (source [_] @v))

(deftype BitVectorSink [v]
  ConsumesBits
  (write! [_ bit]
    (swap! v conj bit))
  (target [_] @v)

  BitCoder
  (finalize! [_] @v))

(deftest arithmetic-coder
  (let [dist (custom-distribution (sorted-map 0 0.05, 1 0.05, 2 0.5, 3 0.4))
        syms [2 3 2 0]
        bts [false true true false true true false false false]]
    (testing "encoding"
      (let [sc (output-stateful-coder (BitVectorSink. (atom [])))]
        (doseq [s syms] (encode-symbol sc dist s))
        (is (= (target (finalize! sc)) bts))))
    (testing "decoding"
      (let [sc (input-stateful-coder (BitVectorSource. (atom bts)))
            out (repeatedly (count syms) #(decode-symbol sc dist))]
        (is (= syms out))))))
