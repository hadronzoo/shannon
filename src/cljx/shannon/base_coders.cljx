(ns shannon.base-coders
  (:require [shannon.coding-primitives :refer [Codeable custom constant decode
                                               encode fixed-array signed
                                               transform uniform variable-array
                                               zipf]]
            [shannon.compatibility :refer [arr->num char->int decompose-date
                                           fInfinity fNaN get-float-parts
                                           int->char is-infinite? is-nan?
                                           num->arr pow recompose-date round]]
            [shannon.ngrams.en :as en]
            #+clj [shannon.switch :refer [switch]]
            #+clj [shannon.math-macros :refer [pow2]])
  #+cljs (:require-macros [shannon.switch :refer [switch]]
                          [shannon.math-macros :refer [pow2]]))

;; coders
(def uint32coder (zipf (pow2 32)))

(def ^:private uniform-uint32coder (uniform (pow2 32)))
(def ^:private lencoder (custom {1 0.6, 2 0.4}))
(def ^:private short64coder uint32coder)
(def ^:private long64coder (fixed-array [uniform-uint32coder uint32coder]))

(deftype UInt64Coder []
  Codeable
  (encode [_ scoder sym]
    (let [arr (num->arr (round sym))
          len (if (zero? (nth arr 1)) 1 2)]
      (encode lencoder scoder len)
      (if (> len 1)
        (encode long64coder scoder arr)
        (encode short64coder scoder (nth arr 0)))))
  (decode [_ scoder]
    (let [len (decode lencoder scoder)
          arr (if (> len 1)
                (decode long64coder scoder)
                [(decode short64coder scoder) 0])]
      (arr->num arr))))

(def uint64coder (UInt64Coder.))

(defn offset-zipf [low mid high]
  (let [total-range (inc (- high low))
        high-range (inc (- high mid))
        low-range (- mid low)
        low-coder (transform (zipf low-range) #(dec (- mid %)) #(- mid (inc %)))
        high-coder (transform (zipf high-range) #(- % mid) #(+ % mid))]

    (switch {:low  {:pr (/ low-range total-range)
                      :test #(< % mid)
                      :coder low-coder}
               :else {:pr (/ high-range total-range)
                      :coder high-coder}})))

(def int32coder (offset-zipf (- (pow2 31)) 0 (dec (pow2 31))))

(def int32coder (signed uint32coder))
(def int64coder (signed uint64coder))

(defn split-float64
  #+clj [^double x]
  #+cljs [^number x]
  (let [[m e] (get-float-parts x)
        [l32 h21] (num->arr m)]
    [(+ e 51) h21 l32]))

(defn join-float64
  #+clj [^long e ^long h21 ^long l32]
  #+cljs [^number e ^number h21 ^number l32]
  (let [m (arr->num [l32 h21])]
    (if (== e -1023)
      (* m (pow2 -1074))
      (+ (pow 2 e) (* (pow 2 (- e 52)) m)))))

(def ^:private small-pr (/ 1 (pow2 32)))
(def ^:private exp (offset-zipf -1023 0 1024))
(def ^:private unif-h21 (uniform (pow2 21)))
(def ^:private unif-l32 (uniform (pow2 32)))
(def ^:private mcoder (fixed-array [unif-h21 unif-l32]))

(deftype UDoubleCoder []
  Codeable
  (encode [_ scoder sym]
    (cond
     (is-nan? sym)
     (do (encode exp scoder 1024)
         (encode mcoder scoder [0 1]))

     (is-infinite? sym)
     (do (encode exp scoder 1024)
         (encode mcoder scoder [0 0]))

     :else
     (let [[e h21 l32] (split-float64 sym)]
       (encode exp scoder e)
       (encode mcoder scoder [h21 l32]))))

  (decode [_ scoder]
    (let [e (decode exp scoder)
          [h21 l32] (decode mcoder scoder)]
      (if (== e 1024)
        (if (zero? l32) fInfinity fNaN)
        (join-float64 e h21 l32)))))

(def doublecoder (signed (UDoubleCoder.)))

(def booleancoder (custom {true 0.5, false 0.5}))

(defn one-indexed [coder] (transform coder dec inc))

(def ^:private year (offset-zipf 0 2015 9999))
(def ^:private month (one-indexed (uniform 12)))
(def ^:private days-in-month {:normal [31 28 31 30 31 30 31 31 30 31 30 31]
                              :leap   [31 29 31 30 31 30 31 31 30 31 30 31]})
(def ^:private days (apply conj {}
                           (map (fn [d]
                                  [d (one-indexed (uniform d))])
                                [28 29 30 31])))
(def ^:private hours (uniform 24))
(def ^:private minutes (uniform 60))
(def ^:private seconds-59 (uniform 60))
(def ^:private seconds-60 (uniform 61))
(def ^:private milliseconds (uniform 1000))

(defn- leap-year? [year]
  (and (zero? (bit-and year 3))
       (or (not (zero? (mod year 25)))
           (zero? (bit-and year 15)))))

(defn- day-coder [year month]
  (let [num-days
        (nth (get days-in-month
                  (if (leap-year? year) :leap :normal))
             (dec month))]
    (get days num-days)))

(defn- sec-coder [mins]
  (if (== mins 59) seconds-60 seconds-59))

(deftype UTCDateCoder []
  Codeable
  (encode [_ scoder sym]
    (let [[yr mo d hr mn s ms] (decompose-date sym)]
      (encode year scoder yr)
      (encode month scoder mo)
      (encode (day-coder yr mo) scoder d)
      (encode hours scoder hr)
      (encode minutes scoder mn)
      (encode (sec-coder mn) scoder s)
      (encode milliseconds scoder ms)))
  (decode [_ scoder]
    (let [yr (decode year scoder)
          mo (decode month scoder)
          d (decode (day-coder yr mo) scoder)
          hr (decode hours scoder)
          mn (decode minutes scoder)
          s (decode (sec-coder mn) scoder)
          ms (decode milliseconds scoder)]
      (recompose-date yr mo d hr mn s ms))))

(def date-coder (UTCDateCoder.))

(def byte-coder (uniform 256))
(def bytes-coder (variable-array byte-coder uint64coder))

(def unicode-char-coder
  (transform (uniform (pow2 16)) char->int int->char))

(defn string-coder [character-coder]
  (transform (variable-array character-coder uint64coder)
             #(map str %) #(apply str %)))

(def unicode-coder (string-coder unicode-char-coder))

(defn unigram-coder [unigrams model-pr]
  (let [ucoder (custom unigrams)]
    (switch {:modeled {:pr model-pr
                         :coder ucoder
                         :test #(contains? unigrams %)}
               :else    {:pr (- 1 model-pr)
                         :coder unicode-char-coder}})))

(defn language-coder [unigrams model-pr]
  (string-coder (unigram-coder unigrams model-pr)))

(def english-coder (language-coder en/unigrams 0.98))

(defn- average-codepoint [s]
  (let [sample (take 20 s)]
    (/ (reduce + (map char->int sample)) (max 1 (count sample)))))

(def adaptive-string-coder
  (switch {:english {:pr 0.8
                       :coder english-coder
                       :test #(< (average-codepoint %) (pow2 7))}
             :else    {:pr 0.2
                       :coder unicode-coder}}))

(def symbol-coder (transform english-coder str symbol))
(def keyword-coder (transform english-coder #(subs (str %) 1) keyword))
(def nil-coder (constant nil))
