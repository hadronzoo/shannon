(ns shannon.base-coders-test
  (:require #+clj [clojure.test :refer [deftest testing is]]
            #+cljs [cemerick.cljs.test :as t]
            [shannon.base-coders :refer [split-float64 join-float64
                                         adaptive-string-coder booleancoder
                                         byte-coder bytes-coder date-coder
                                         doublecoder english-coder int32coder
                                         int64coder keyword-coder nil-coder
                                         symbol-coder uint32coder uint64coder
                                         unicode-char-coder unicode-coder]]
            [shannon.compatibility :refer [fInfinity now pow round]]
            [shannon.compressor :refer [compress decompress]])
  #+cljs (:require-macros [cemerick.cljs.test :refer [is deftest with-test
                                                      run-tests testing]]))

(defn- test-coder [coder values]
  (doseq [v values]
    (let [v2 (-> (compress v coder)
                 (decompress coder))]
      (is (= v v2)))))

(deftest utility-functions
  (testing "split-float64"
    (testing "min subnormal double"
      (is (= [-1023 0 1] (split-float64 4.9406564584124654E-324))))
    (testing "max subnormal double"
      (is (= [-1023 1048575 4294967295] (split-float64 2.225073858507201E-308))))
    (testing "min normal double"
      (is (= [-1022 0 0] (split-float64 2.2250738585072014e-308))))
    (testing "low real"
      (is (= [-972 240853 4137376210] (split-float64 3.0806478678099587e-293))))
    (testing "another low real"
      (is (= [-202 570317 2495640414] (split-float64 2.4019240112727754e-61))))
    (testing "zero"
      (is (= [-1023 0 0] (split-float64 0.0))))
    (testing "real"
      (is (= [3 94889 2148723838] (split-float64 8.723949434576067))))
    (testing "max double"
      (is (= [1023 1048575 4294967295] (split-float64 1.7976931348623157e308)))))
  (testing "join-float64"
    (testing "min subnormal double"
      (is (== 4.9406564584124654E-324 (join-float64 -1023 0 1))))
    (testing "max subnormal double"
      (is (== 2.225073858507201E-308 (join-float64 -1023 1048575 4294967295))))
    (testing "min normal double"
      (is (== 2.2250738585072014e-308 (join-float64 -1022 0 0))))
    (testing "low real"
      (is (== 3.0806478678099587e-293 (join-float64 -972 240853 4137376210))))
    (testing "another low real"
      (is (== 2.4019240112727754e-61 (join-float64 -202 570317 2495640414))))
    (testing "zero"
      (is (== 0.0 (join-float64 -1023 0 0))))
    (testing "real"
      (is (== 8.723949434576067 (join-float64 3 94889 2148723838))))
    (testing "max double"
      (is (== 1.7976931348623157e308 (join-float64 1023 1048575 4294967295)))))
  (testing "random split-64/join-64"
    (doseq [n (repeatedly 10 #(* (rand) (pow 2 (if (pos? (rand-int 1))
                                                 (rand-int 308)
                                                 (- (rand-int 324))))))]
      (is (== n (apply join-float64 (split-float64 n)))))))

  (deftest atom-coders
    (testing "32-bit unsigned int"
      (test-coder uint32coder [0 (round (pow 2 31)) (round (dec (pow 2 32)))]))
    (testing "32-bit signed int"
      (test-coder int32coder [(round (- (pow 2 31))) 0 (round (dec (pow 2 31)))]))
    (testing "64-bit unsigned int"
      (test-coder uint64coder [0 (round (pow 2 40)) (round (pow 2 53))]))
    (testing "64-bit signed int"
      (test-coder int64coder [(round (- (pow 2 53))) 0 (round (pow 2 53))]))
    (testing "double"
      (test-coder doublecoder
                  [0.0 1.0000000000000002 fInfinity ;; special values
                   -3.0806478678099587e-293 -2.4019240112727754e-61 8.242649903982319e187 ;; random values
                   4.9406564584124654e-324 2.2250738585072009e-308 ;; subnormals
                   2.2250738585072014e-308 1.7976931348623157e308 ;; min and max
                   ]))
    (testing "boolean"
      (test-coder booleancoder [true false]))
    (testing "date-coder"
      (test-coder date-coder [#inst "1600-02-29T01:00:00.000Z"
                              #inst "2004-01-01T00:00:00.000Z"
                              #inst "2024-12-31T23:59:59.999Z"
                              (now)]))
    (testing "byte-coder"
      (test-coder byte-coder [0 128 255]))
    (testing "bytes-coder"
      (test-coder bytes-coder [[0 0 0] [138 24 31 198 0] [255 255 255]]))
    (testing "unicode character coder"
      (test-coder unicode-char-coder ["0" "a" "Z" "ꑡ" "ꐺ" "ꑩ" "ꎕ" "ᔡ" "ಹ" "ㄽ" "⦬" "ਦ"]))
    (testing "unicode string coder"
      (test-coder unicode-coder ["兵者，詭道也。故能而示之不能，用而示之不用，近而示之遠，遠而示之近"]))
    (testing "english string coder"
      (test-coder english-coder ["Await no further word or sign from me:\nyour will is free, erect, and whole – to act\nagainst that will would be to err: therefore\nI crown and miter you over yourself."]))
    (testing "adaptive string coder"
      (test-coder adaptive-string-coder ["پارسی گو گرچه تازی خوشتر است — عشق را خود صد زبان دیگر است"
                                         "The reformer is always right about what is wrong. He is generally wrong about what is right."]))
    (testing "symbol-coder"
      (test-coder symbol-coder [(symbol "test") (symbol "ns/test")]))
    (testing "keyword-coder"
      (test-coder keyword-coder [:test :ns/test]))
    (testing "nil-coder"
      (test-coder nil-coder [nil])))
