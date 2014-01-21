(ns shannon.compatibility
  (:require #+clj  [clj-time.core :as time]
            #+clj  [clj-time.coerce :refer [to-date from-date]]
            #+cljs [goog.date :as date]
            #+cljs [goog.date.UtcDateTime])
  (#+clj :use #+cljs :require-macros [shannon.math-macros :as m])
  #+clj (import [java.util Date]))

#+clj (set! *unchecked-math* true)

;; Exceptions
(def exception
  #+clj Exception
  #+cljs js/Error)

;; Characters (characters are strings of length one for compatibility with javascript)
(defn char->int [s]
  #+clj (int (first (str s)))
  #+cljs (.charCodeAt s 0))

(defn int->char [i]
  #+clj (str (char i))
  #+cljs (.fromCharCode js/String i))

;; Tests
(def is-float?
  #+clj float?
  #+cljs (every-pred number? (comp not integer?)))
(def is-string?
  #+clj string?
  #+cljs (every-pred string? (comp not keyword?)))

#+clj (defn is-date? [d]
        (let [t (type d)]
          (or (= t java.util.Date)
              (= t org.joda.time.DateTime))))
#+cljs (defn is-date? [d] (= (type d) js/Date))

(defn is-boolean? [x]
  (= (boolean x) x))

;; Math

#+clj (defn is-nan? [^double x] (Double/isNaN x))
#+cljs (defn is-nan? [^number x] (js/isNaN x))

#+clj (defn is-infinite? [^double x] (Double/isInfinite x))
#+cljs (defn is-infinite? [^number x] (not (js/isFinite x)))

(def fNaN
  #+clj (/ 0.0 0.0)
  #+cljs js/NaN)

(def fInfinity
  #+clj (/ 1.0 0.0)
  #+cljs js/Infinity)

(def pi
  #+clj Math/PI
  #+cljs (.-PI js/Math))

#+clj (defn pow [n m] (Math/pow n m))
#+cljs (defn pow [n m] (.pow js/Math n m))

#+clj (defn atan [^double x] (Math/atan x))
#+cljs (defn atan [^number x] (.atan js/Math x))

#+clj (defn ln [^double x] (Math/log x))
#+cljs (defn ln [^number x] (.log js/Math x))

#+clj (defn sin [^double x] (Math/sin x))
#+cljs (defn sin [^number x] (.sin js/Math x))

#+clj (defn abs [^double x] (Math/abs x))
#+cljs (defn abs [^number x] (.abs js/Math x))

#+clj (defn floor [^double x] (Math/floor x))
#+cljs (defn floor [^number x] (.floor js/Math x))

#+clj (defn round [^double x] (Math/round x))
#+cljs (defn round [^number x] (.round js/Math x))

(defn roundd [^double x]
  (double (round x)))

#+clj (defn ceiling [^double x] (Math/ceil x))
#+cljs (defn ceiling [^number x] (.ceil js/Math x))

;; working with longs
#+clj
(defn num->arr [^long n]
  [(bit-and n 0xffffffff) (bit-shift-right n 32)])

#+cljs
(defn num->arr [^number n]
  (let [mag (m/pow2 32)]
    [(mod n mag) (floor (/ n mag))]))

#+clj
(defn arr->num [[^long low ^long high]]
  (bit-or (bit-shift-left high 32) low))

#+cljs
(defn arr->num [[^number low ^number high]]
  (+ (* high (m/pow2 32)) low))

;; float processing
#+clj
(defn get-float-parts [^double x]
  (let [bits (Double/doubleToLongBits x)
        exp (bit-and (bit-shift-right bits 52) 0x7ff)
        man (bit-and bits 0xfffffffffffff)]
    (if (zero? exp)
      [man -1074]
      [man (- exp 1074)])))

#+cljs
(defn get-float-parts [^number x]
  (letfn
    [(mantissa [exp x]
               (round (* (- (/ x (pow 2 (+ exp 51))) 1) 4503599627370496)))
     (exponent [x]
               (let [exp (min 1023 (max -1023 (round (* 1.4426950408889634074 (ln x)))))
                     man (/ x (pow 2 exp))]
                 (loop [r man, e exp]
                   (cond
                    (or (< e -1022) (> e 1022)) (- e 51)
                    (< r 1) (recur (* r 2) (dec e))
                    (>= r 2) (recur (/ r 2) (inc e))
                    :else (- e 51)))))]
    (if (< x 2.2250738585072014e-308)
      [(round (/ x (m/pow2 -1074))) -1074]
      (let [exp (exponent x)
            man (mantissa exp x)]
        [man exp]))))

;; Dates
(defn now []
  #+clj (Date.)
  #+cljs (js/Date.))

#+clj
(defn decompose-date [d]
  (let [d (if (= (type d) org.joda.time.DateTime) d (from-date d))
        utcd (time/from-time-zone d time/utc)]
    ((juxt time/year
           time/month
           time/day
           time/hour
           time/minute
           time/second
           time/milli) utcd)))

#+cljs
(defn decompose-date [d]
  (let [d (date/UtcDateTime. d)]
    [(.getFullYear d)
     (inc (.getUTCMonth d))
     (.getUTCDate d)
     (.getUTCHours d)
     (.getUTCMinutes d)
     (.getUTCSeconds d)
     (.getUTCMilliseconds d)]))

(defn recompose-date [year month day hours minutes seconds millisec]
  #+clj
  (to-date (time/date-time year month day hours minutes seconds millisec))

  #+cljs
  (js/Date. (.getTime (date/UtcDateTime. year (dec month) day hours minutes seconds millisec))))
