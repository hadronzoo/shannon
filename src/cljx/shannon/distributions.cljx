(ns shannon.distributions
  (:require [shannon.compatibility :refer [abs atan ceiling floor
                                           ln pi pow round sin]]))

#+clj (set! *unchecked-math* true)

(defn harmonic-number
  #+clj [^double n]
  #+cljs [^number n]
  (if (<= n 0) 0
      (letfn [(exact
               #+clj [^long n]
               #+cljs [^number n]
                (loop [i 1. v 0.]
                  (if (<= i n)
                    (recur (+ i 1.) (+ v (/ i)))
                    v)))
              (approx
               #+clj [^double n]
               #+cljs [^number n]
                (let [a (+ 1. n)
                      b (* 2. n)
                      c (+ 1. b)
                      d (+ c (* 2. (pow n 2.)))
                      e (* 6. n)
                      f (pow n 2.)]
                  (+ -1
                     0.57721566490153286060651209008240243104215933593992
                     (- (/ (+ 1. e (* 6. f))
                           (* 180. (pow d 3))))
                     (/ (+ -1.
                           (* -42. f)
                           (* -14. n)
                           (* 140. (pow n 4.))
                           (* 168. (pow n 5.))
                           (* 56. (pow n 6.)))
                        (* 2520. (pow d 7.)))
                     (* c (atan (/ c)))
                     (* 0.5 (ln (/ d 2.))))))]
        (if (< n 50)
          (let [n0 (floor n)
                y0 (exact n0)]
            (if (== n n0)
              y0
              (let [n1 (+ n0 1.)
                    y1 (+ y0 (/ n1))
                    a (- n1 n)
                    b (- 1. a)]
                (+ (* a y0) (* b y1)))))
          (approx n)))))

(def ^:private nth-harmonic (memoize harmonic-number))

(defn zipf-pmf
  #+clj [^long k ^long N]
  #+cljs [^number k ^number N]
  (cond
   (< k 1) 0.
   (> k N) 0.
   :else (/ 1. k (nth-harmonic N))))

(defn zipf-cdf
  #+clj [^long k ^long N]
  #+cljs [^number k ^number N]
  (cond
   (<= k 0) 0.
   (>= k N) 1.
   :else (/ (harmonic-number k) (nth-harmonic N))))

(def ^:private zipf-breakpoint (memoize zipf-cdf))

(defn inverse-zipf-cdf
  #+clj [^double p ^long N]
  #+cljs [^number p ^number N]
  (letfn [(newton
           #+clj [^double x]
           #+cljs [^number x]
            (let [a (+ 1. x)
                  b (* a x)
                  c (pow (+ 1. (* 2. b)) 8.)
                  d (+ 1. (* 2. x))]
              (+ x
                 (/ (* 45. c (- (* p (nth-harmonic N)) (harmonic-number x)))
                    (+ (* 8. b d (+ 1. (* b (+ 7. (* b (+ 17. (* 12. b (+ 2. x (pow x 2.)))))))))
                       (* 90. c (atan (/ d))))))))]
    (cond
     (<= p 0) 0
     (>= p 1) N

     (< p (zipf-breakpoint 50 N))
     (loop [k 1 pk (zipf-pmf 1 N)]
       (if (< pk p)
         (recur (inc k) (+ pk (zipf-pmf (inc k) N)))
         (dec k)))

     :else
     (let [guess 50.
           epsilon 0.5]
       (loop [x1 guess x0 0. i 0]
         (cond (> i 100) nil
               (> (abs (- x1 x0)) epsilon) (recur (double (newton x1)) x1 (inc i))
               :else (round x1)))))))

(defprotocol DiscreteDistribution
  (next-lower [o k])
  (next-higher [o k])
  (cdf
   #+clj [o k]
   #+cljs [o k])
  (inverse-cdf
   #+clj [o interval]
   #+cljs [o interval]))

(defn- check-bounded-cdf [k N]
  {:pre [(integer? k) (<= 0 k (dec N))]})

(defn- check-inverse-cdf [p]
  {:pre [(number? p) (<= 0 p 1)]})

(deftype ZipfDistribution [N]
  DiscreteDistribution
  (next-lower [_ k]
    (when-not (zero? k) (dec k)))

  (next-higher [_ k]
    (when-not (>= k (dec N)) (inc k)))

  (cdf [_ k]
    (check-bounded-cdf k N)
    [(zipf-cdf k N) (zipf-cdf (inc k) N)])

  (inverse-cdf [_ p]
    (check-inverse-cdf p)
    (inverse-zipf-cdf p N)))

(defn zipf-distribution [N] (ZipfDistribution. N))



(deftype UniformDistribution [N]
  DiscreteDistribution
  (next-lower [_ k]
    (when-not (zero? k) (dec k)))

  (next-higher [_ k]
    (when-not (>= k (dec N)) (inc k)))

  (cdf [_ k]
    (check-bounded-cdf k N)
    [(/ k N) (/ (inc k) N)])

  (inverse-cdf [_ p]
    (check-inverse-cdf p)
    (round (floor (* N p)))))

(defn uniform-distribution [N] (UniformDistribution. N))



(defn- gcdf
  #+clj [^long k ^double mu]
  #+cljs [^number k ^number mu]
  (- 1. (pow (/ mu (+ 1. mu))
            (+ 1. (floor k)))))

(deftype GeometricDistribution [mu]
  DiscreteDistribution
  (next-lower [_ k]
    (when-not (zero? k) (dec k)))

  (next-higher [_ k]
    (inc k))

  (cdf [_ k]
    {:pre [(>= k 0)]}
    [(gcdf (dec k) mu) (gcdf k mu)])

  (inverse-cdf [_ p]
    (check-inverse-cdf p)
    (ceiling (- (/ (ln (- 1. p))
                   (ln (/ mu (+ 1. mu)))) 1.))))

(defn geometric-distribution [mu] (GeometricDistribution. mu))



(defn ln-gamma
  #+clj [^double z]
  #+cljs [^number z]
  (let [ln-sqrt-2-pi 0.91893853320467274178
        g 9.
        lanczos-coef [1.000000000000000174663
                      5716.400188274341379136
                      -14815.30426768413909044
                      14291.49277657478554025
                      -6348.160217641458813289
                      1301.608286058321874105
                      -108.1767053514369634679
                      2.605696505611755827729
                      -0.7423452510201416151527e-2
                      0.5384136432509564062961e-7
                      -0.4023533141268236372067e-8]]
    (if (< z 0.5)
      (- (ln (/ pi (sin (* pi z))))
         (ln-gamma (- 1. z)))
      (let [z (- z 1.)
            base (+ z g 0.5)]
        (loop [sum 0., i (dec (count lanczos-coef))]
          (if (>= i 1)
            (recur (+ sum (/ (nth lanczos-coef i)
                             (+ z i)))
                   (dec i))
            (let [sum (+ sum (nth lanczos-coef 0))]
              (+ ln-sqrt-2-pi (ln sum) (- base) (* (ln base) (+ z 0.5))))))))))

(defn benford-distribution
  #+clj [^long base ^long digits]
  #+cljs [^number base ^number digits]
  (let [min_d (pow base (dec digits))
        max_d (dec (pow base digits))
        ln-base (ln base)
        gamma-d1 (ln-gamma min_d)
        gamma-d2 (ln-gamma (+ 1. min_d))
        epsilon 0.5]
    (letfn
        [(benford-cdf
          #+clj [^long d]
          #+cljs [^number d]
           (cond
            (< d min_d) 0.
            (>= d max_d) 1.
             :else
             (/ (+ (- (ln-gamma (+ 1. d)))
                   gamma-d1
                   (ln-gamma (+ 2. d))
                   (- gamma-d2))
                ln-base)))
         (newton
          #+clj [^double d ^double p]
          #+cljs [^number d ^number p]
           (let [a (+ d 1.)]
             (+ d (- (* a (ln a))) (* a (+ digits p -1.) ln-base))))
         (benford-inverse-cdf
          #+clj [^double p ^double d0]
          #+cljs [^number p ^number d0]
           (loop [i 1 d d0]
             (let [dn (max min_d (min max_d (newton d p)))]
               (cond
                (>= i 100) nil
                (< (abs (- dn d)) epsilon) (round dn)
                :else (recur (inc i) (double (max (min dn max_d) min_d)))))))]
      (let [d0 (benford-inverse-cdf 0.5 min_d)]
        (reify DiscreteDistribution
          (next-lower [_ d]
            (when-not (<= d min_d) (dec d)))

          (next-higher [_ d]
            (when-not (>= d max_d) (inc d)))

          (cdf [_ d]
            {:pre [(<= min_d d max_d)]}
            [(benford-cdf (dec d)) (benford-cdf d)])

          (inverse-cdf [_ p]
            (check-inverse-cdf p)
            (benford-inverse-cdf p d0)))))))

;;;

(defn- pdf->cdf [prs]
  (zipmap (keys prs)
          (reduce (fn [cdf p]
                    (let [last (peek cdf)
                          last-p (when last (nth last 1))
                          interval (if last-p [last-p (+ last-p p)] [0 p])]
                      (conj cdf interval)))
                  [] (vals prs))))

(defn- invert-map
  ([m] (invert-map m identity))
  ([m f] (zipmap (map f (vals m)) (keys m))))

(defn custom-distribution [prs]
  (let [nzprs (select-keys prs (for [[k v] prs :when (pos? v)] k))
        norm-c (/ (reduce + (vals nzprs)))
        pdf (into (sorted-map)
                  (zipmap (keys nzprs) (map #(+ (* norm-c %)) (vals nzprs))))
        cdf (pdf->cdf pdf)
        inverse (into (sorted-map) (invert-map cdf second))
        key-vec (vec (vals inverse))
        key-indices (apply conj {} (map-indexed (fn [i x] [x i]) key-vec))]
    (reify DiscreteDistribution
      (next-higher [_ x]
        (let [i (get key-indices x)]
          (when (< i (dec (count key-vec))) (nth key-vec (inc i)))))
      (next-lower [_ x]
        (let [i (get key-indices x)]
          (when-not (zero? i) (nth key-vec (dec i)))))
      (cdf [_ x]
        (get cdf x))
      (inverse-cdf [_ p]
        (second (first (subseq inverse >= (+ p 0.0000000000000002))))))))

(deftype ConstantDistribution [value]
  DiscreteDistribution
  (next-higher [_ _] nil)
  (next-lower [_ _] nil)
  (cdf [_ _] [0 1])
  (inverse-cdf [_ _] value))

(defn constant-distribution [value] (ConstantDistribution. value))
