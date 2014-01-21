(ns shannon.distributions-test
  (:require #+clj [clojure.test :refer [deftest testing is]]
            #+cljs [cemerick.cljs.test :as t]
            [shannon.compatibility :refer [abs pow]]
            [shannon.distributions :refer [harmonic-number zipf-pmf zipf-cdf
                                           inverse-zipf-cdf ln-gamma next-lower
                                           next-higher cdf inverse-cdf
                                           zipf-distribution uniform-distribution
                                           custom-distribution]])
  #+cljs (:require-macros [cemerick.cljs.test :refer [is deftest with-test
                                                      run-tests testing]]))

(defn- close-enough?
  ([actual expected] (close-enough? 1e-14 actual expected))
  ([epsilon actual expected]
     (< (abs (- expected actual)) epsilon)))

(defn- interval-close?
  [a b]
  (reduce #(and %1 %2) (map close-enough? a b)))

(defn- test-cdfs
  [defs]
  (doseq [{:keys [dist tests]} defs]
    (doseq [{:keys [value actual approx]} tests]
      (is (interval-close? (cdf dist value) actual))
      (is (map == (cdf dist value) approx)))))

(defn- mid-interval
  [[a b]]
  (+ a (* 0.5 (- b a))))

(defn- cdf-dual?
  [dist N]
  (= N (inverse-cdf dist (mid-interval (cdf dist N)))))

(defn- test-duals
  [defs]
  (doseq [{:keys [dist values]} defs]
    (doseq [v values]
      (is (cdf-dual? dist v)))))

(defn- is-ordered?
  [dist [a1 a2 :as a] [b1 b2 :as b]]
  (letfn
    [(check-interval
      [[a1 a2]]
      (is (and (= (next-higher dist a1) a2)
               (= (next-lower dist a2) a1))))]
    (is (nil? (next-lower dist a1)))
    (is (nil? (next-higher dist b2)))
    (check-interval a)
    (check-interval b)))

;; Actual numeric test values generated using Mathematica

(deftest utilities
  (testing "zipf utilities"
    ;; Harmonic Numbers
    (testing "actual harmonic numbers"
      (is (close-enough? (harmonic-number       0)     0.0))
      (is (close-enough? (harmonic-number      49)     4.4792053383294250576))
      (is (close-enough? (harmonic-number 1000000)    14.392726722865723631))
      (is (close-enough? (harmonic-number (pow 2 32)) 22.757925442936198083780)))
    (testing "harmonic number approximations"
      (is (== (harmonic-number       0)     0.0))
      (is (== (harmonic-number      49)     4.4792053383294235))
      (is (== (harmonic-number 1000000)    14.392726722865724))
      (is (== (harmonic-number (pow 2 32)) 22.757925442936198)))

    ;; PMF
    (testing "actual PMF"
      (is (close-enough? (zipf-pmf   1  32) 0.24639674358230769525))
      (is (close-enough? (zipf-pmf  32  32) 0.0076998982369471154765))
      (is (close-enough? (zipf-pmf 100 200) 0.0017012499743976156738)))
    (testing "PMF approximations"
      (is (== (zipf-pmf   1  32) 0.2463967435823077))
      (is (== (zipf-pmf  32  32) 0.007699898236947116))
      (is (== (zipf-pmf 100 200) 0.0017012499743976157)))

    ;; CDF
    (testing "actual CDF"
      (is (close-enough? (zipf-cdf   1 32)  0.24639674358230769525))
      (is (close-enough? (zipf-cdf  32 32)  1.0))
      (is (close-enough? (zipf-cdf 100 200) 0.88250258690751711172)))
    (testing "CDF approximations"
      (is (== (zipf-cdf   1 32)  0.2463967435823077))
      (is (== (zipf-cdf  32 32)  1.0))
      (is (== (zipf-cdf 100 200) 0.882502586907517)))

    ;; Inverse CDF
    (testing "actual inverse CDF"
      (is (== (inverse-zipf-cdf 0.24639674381513833890  32)   1))
      (is (== (inverse-zipf-cdf 1.0                     32)  32))
      (is (== (inverse-zipf-cdf 0.88250258714034775537 200) 100))))
  
  (testing "Benford utilities"
    (testing "log-gamma"
      (is (close-enough? (ln-gamma 1e-6)  13.815509980749431669))
      (is (close-enough? (ln-gamma 0.1)    2.2527126517342059599))
      (is (close-enough? (ln-gamma 0.5)    0.57236494292470008707))
      (is (close-enough? (ln-gamma 10)    12.801827480081469611))
      (is (close-enough? (ln-gamma 1e3) 5905.2204232091812118))
      (is (close-enough? (ln-gamma 1e6)    1.2815504569147611660e7)))))

(deftest distributions
  (testing "zipf distribution"
    (testing "CDF"
      (test-cdfs [{:dist (zipf-distribution 32)
                   :tests [{:value  0
                            :actual [0 0.24639674358230769525]
                            :approx [0 0.2463967435823077]}
                           {:value  31
                            :actual [0.99230010176305288452 1]
                            :approx [0.9923001017630528     1]}]}
                  {:dist (zipf-distribution 1048576)
                   :tests [{:value  0
                            :actual [0 0.069251311419638053970]
                            :approx [0 0.06925131141963806]}
                           {:value  1048575
                            :actual [0.99999993395680292164 1]
                            :approx [0.9999999339568029     1]}]}]))
    (testing "inverse CDF"
      (test-duals [{:dist (zipf-distribution 32)
                    :values [0 31]}
                   {:dist (zipf-distribution 1048576)
                    :values [0 1048575]}]))
    (testing "symbol order"
      (is-ordered? (zipf-distribution 32) [0 1] [30 31])
      (is-ordered? (zipf-distribution 1048576) [0 1] [1048574 1048575])))
  (testing "uniform distribution"
    (testing "CDF"
      (test-cdfs [{:dist (uniform-distribution 32)
                   :tests [{:value  0
                            :actual [0 0.031250000000000000000]
                            :approx [0 0.03125]}
                           {:value  31
                            :actual [0.96875000000000000000 1]
                            :approx [0.96875                1]}]}
                  {:dist (uniform-distribution 1048576)
                   :tests [{:value  0
                            :actual [0 9.5367431640625000000e-7]
                            :approx [0 9.5367431640625E-7]}
                           {:value  1048575
                            :actual [0.99999904632568359375 1]
                            :approx [0.9999990463256836     1]}]}]))
    (testing "inverse CDF"
      (test-duals [{:dist (uniform-distribution 32)
                    :values [0 31]}
                   {:dist (zipf-distribution 1048576)
                    :values [0 1048575]}]))
    (testing "symbol order"
      (is-ordered? (uniform-distribution 32) [0 1] [30 31])
      (is-ordered? (uniform-distribution 1048576) [0 1] [1048574 1048575])))
  (testing "custom distribution"
    (let [m (custom-distribution (sorted-map :a 0.1, :b 0.6, :c 0.3))]
      (testing "CDF"
        (test-cdfs [{:dist m
                     :tests [{:value :a
                              :actual [0 0.1]
                              :approx [0 0.1]}
                             {:value :b
                              :actual [0.1 0.7]
                              :approx [0.1 0.7]}
                             {:value :c
                              :actual [0.7 1]
                              :approx [0.7 1]}]}]))
      (testing "inverse CDF"
        (test-duals [{:dist m :values [:a :b :c]}]))
      (testing "symbol order"
        (is-ordered? m [:a :b] [:b :c])))))
