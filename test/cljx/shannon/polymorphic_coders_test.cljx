(ns shannon.polymorphic-coders-test
  (:require #+clj [clojure.test :refer [deftest testing is]]
            #+cljs [cemerick.cljs.test :as t]
            [shannon.compatibility :refer [now]]
            [shannon.compressor :refer [compress decompress]])
  #+cljs (:require-macros [cemerick.cljs.test :refer [is deftest with-test
                                                      run-tests testing]]))

(defn- test-polycoders [values]
  (doseq [v values]
    (let [v2 (-> (compress v)
                 (decompress))]
      (is (= v v2)))))

(deftest polymorphic-coders
  (let [v [1 2.3 nil {:a #{"s1" (now) ()}}]]
    (test-polycoders [nil true (symbol "sym") (now) 1 1.1 "test" :test
                      (list 1 2 3) (list)
                      [1 2 3]
                      (array-map :a 1, :b 2)
                      {:a 1, :b 2}
                      (sorted-map :a 1, :b 2)
                      (array-map :a 1, :b 2)
                      #{:a :b :c}
                      (sorted-set :a :b :c)
                      [0.9211893089051543 0.06123623137225609
                       0.934003247209872 0.26874988980709136]
                      (vec (range 1000))
                      v])
    (is (= v (decompress (compress v))))))
