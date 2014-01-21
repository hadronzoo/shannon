(ns shannon.core
  (:require [shannon.compatibility :refer [pow roundd]]
            [shannon.distributions :refer [cdf inverse-cdf next-higher
                                           next-lower]]
            [shannon.io :refer [BitCoder default-bit-sink finalize! read!
                                with-bits! write!]])
  #+clj (:import [java.io Closeable]))

#+clj (set! *unchecked-math* true)

;;;

(def ^:private coding-precision 48)
(def ^:private whole   (pow 2. coding-precision))
(def ^:private half    (/ whole 2.))
(def ^:private quarter (/ whole 4.))

;;;

(defprotocol DistributionCoder
  (encode-symbol [c distribution sym])
  (decode-symbol [c distribution]))

;;;

(defn- update-interval
  #+clj [[^double la ^double lb] [^double cx ^double dx]]
  #+cljs [[^number la ^number lb] [^number cx ^number dx]]
  (if (not-any? nil? [cx dx])
    (let [w (- lb la)]
      [(+ la (roundd (* w (/ cx whole))))
       (+ la (roundd (* w (/ dx whole))))])
    [la lb]))

(defn- rescale-interval
  #+clj [[^double a ^double b]]
  #+cljs [[^number a ^number b]]
  (let [a (roundd (* whole a))
        b (roundd (* whole b))]
    (cond
     (zero? a) [a (dec b)]
     (== b whole) [(inc a) b]
     :else [(inc a) (dec b)])))

(defn- rescaled-inverse-cdf
  #+clj [distribution ^double znorm]
  #+cljs [distribution ^number znorm]
  (let [z (roundd (* whole znorm))]
    (loop [i (inverse-cdf distribution znorm)]
      (let [[a b :as interval] (rescale-interval (cdf distribution i))]
        (cond
         (< z (dec a)) (recur (next-lower distribution i))
         (> z (inc b)) (recur (next-higher distribution i))
         :else [i interval])))))

(defn- emit-bits
  #+clj [bits ^Boolean i ^long s]
  #+cljs [bits ^boolean i ^number s]
  (let [r (not i)]
    (write! bits i)
    (loop [j s]
      (if (pos? j)
        (do (write! bits r)
            (recur (dec j))))))
  bits)

(deftype ArithmeticCoder #+clj [^double a ^double b ^long s ^double z bits]
                         #+cljs [^number a ^number b ^number s ^number z bits]
  DistributionCoder
  (encode-symbol [_ distribution sym]
    (let [[a b] (->> (cdf distribution sym)
                     (rescale-interval)
                     (update-interval [a b]))]
      (loop [a a b b s s bits bits]
        (cond
         (< b half)
         (recur (* 2. a) (* 2. b) 0
                (emit-bits bits false s))

         (> a half)
         (recur (* 2. (- a half)) (* 2. (- b half)) 0
                (emit-bits bits true s))

         :else
         (loop [a a b b s s]
           (if (and (> a quarter)
                    (< b (- whole quarter)))
             (recur (* 2. (- a quarter))
                    (* 2. (- b quarter)) (inc s))
             (ArithmeticCoder. a b s z bits)))))))

  (decode-symbol [_ distribution]
    (letfn [(update-z [z bits] (if (read! bits) (inc z) z))]
      (let [znorm (/ (- z a) (- b a))
            [sym cdf-interval] (rescaled-inverse-cdf distribution znorm)
            [a b] (update-interval [a b] cdf-interval)]
        (loop [a a b b z z]
          (cond
           (< b half)
           (recur (* 2. a) (* 2. b)
                  (double (update-z (* 2. z) bits)))

           (> a half)
           (recur (* 2. (- a half)) (* 2. (- b half))
                  (double (update-z (* 2. (- z half)) bits)))

           :else
           (loop [a a b b z z]
             (if (and (> a quarter) (< b (- whole quarter)))
               (recur (* 2. (- a quarter)) (* 2. (- b quarter))
                      (double (update-z (* 2. (- z quarter)) bits)))
               [sym (ArithmeticCoder. a b s z bits)])))))))

  BitCoder
  (with-bits! [_ newbits]
    (loop [i (dec coding-precision) z 0.]
      (if (>= i 0)
        (recur (dec i) (+ z (if (read! newbits) (pow 2 i) 0.)))
        (ArithmeticCoder. a b s z newbits))))

  (finalize! [_]
    (let [s (inc s)]
      (if (<= a quarter)
        (emit-bits bits false s)
        (emit-bits bits true s))
      (finalize! bits)
      bits))

  #+clj Closeable
  #+clj (close [_] (when (instance? Closeable bits) (.close bits))))

(deftype StatefulCoder [coder]
  DistributionCoder
  (encode-symbol [_ distribution sym]
    (swap! coder (fn [coder]
                   (encode-symbol coder distribution sym))))
  (decode-symbol [_ distribution]
    (let [sym (atom nil)]
      (swap! coder (fn [coder]
                     (let [[s c] (decode-symbol coder distribution)]
                       (reset! sym s)
                       c)))
      @sym))

  BitCoder
  (finalize! [_] (finalize! @coder))

  #+clj Closeable
  #+clj (close [_] (.close @coder)))

(defn- empty-coder
  ([] (ArithmeticCoder. 0. whole 0 0. nil))
  ([bit-io] (ArithmeticCoder. 0. whole 0 0. bit-io)))

(defn input-stateful-coder [source]
  (StatefulCoder. (atom (with-bits! (empty-coder) source))))

(defn output-stateful-coder
  ([] (StatefulCoder. (atom (empty-coder (default-bit-sink)))))
  ([sink] (StatefulCoder. (atom (empty-coder sink)))))
