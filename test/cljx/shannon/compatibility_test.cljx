(ns shannon.compatibility-test
  (:require #+clj [clojure.test :refer [deftest testing is]]
            #+cljs [cemerick.cljs.test :as t]
            [shannon.compatibility :refer [arr->num num->arr]])
  #+cljs (:require-macros [cemerick.cljs.test :refer [is deftest with-test
                                                      run-tests testing]]))

(deftest utility-functions
  (testing "num->arr"
    (is (= [0 0] (num->arr 0)))
    (is (= [0 1] (num->arr 4294967296)))
    (is (= [1 1] (num->arr 4294967297)))
    (is (= [4294967295 2097151] (num->arr 9007199254740991))))
  (testing "arr->num"
    (is (= 0 (arr->num [0 0])))
    (is (= 4294967296 (arr->num [0 1])))
    (is (= 4294967297 (arr->num [1 1])))
    (is (= 9007199254740991 (arr->num [4294967295 2097151])))))
