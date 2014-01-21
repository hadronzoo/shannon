(ns shannon.coding-primitives-test
  (:require [shannon.compatibility :refer [round pow]]
            [shannon.compressor :refer [compress decompress]]
            [shannon.coding-primitives :refer [encode decode uniform zipf custom
                                               constant fixed-array transform
                                               sparse-array variable-array]]
            #+clj [clojure.test :refer [deftest testing is]]
            #+cljs [cemerick.cljs.test :as t])
  #+cljs (:require-macros [cemerick.cljs.test :refer [is deftest with-test
                                                      run-tests testing]]))

(defn- test-coder [coder values]
  (doseq [v values]
    (let [v2 (-> (compress v coder)
                 (decompress coder))]
      (is (= v v2)))))

(deftest distribution-coders
  (let [N (round (pow 2 32))
        numeric-values [0 (round (pow 2 31)) (dec (round (pow 2 32)))]]
    (testing "uniform" (test-coder (uniform N) numeric-values))
    (testing "zipf" (test-coder (zipf N) numeric-values))
    (testing "custom" (test-coder (custom {:a 0.01 :b 0.1 :c 0.89}) [:a :b :c]))
    (testing "constant" (test-coder (constant :c) [:c]))))

(deftest composite-coders
  (testing "fixed array"
    (let [cnt 10, N 100]
      (test-coder (fixed-array (repeat cnt (uniform N)))
                  [(repeatedly cnt #(rand-int N))])))
  (testing "transform"
    (test-coder (transform (uniform 1) inc dec) [-1]))
  (testing "variable array"
    (test-coder (variable-array (uniform 10) (uniform 10)) [(range 7)]))
  (testing "sparse array"
    (test-coder (sparse-array 10 (constant true)) [{3 true, 7 true}])))
