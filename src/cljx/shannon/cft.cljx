(ns shannon.cft
  (:require [shannon.distributions :refer [DiscreteDistribution cdf inverse-cdf
                                           next-higher next-lower]]
            [shannon.compatibility :refer [round floor]]))

(defprotocol AdaptiveDiscreteDistribution
  (inc-symbol [o sym] [o sym cnt])
  (symbol-count [o sym])
  (total-count [o]))

(defn- table-indices [N index testfn]
  (for [i (range (inc N))
        :when (testfn (bit-test index i))
        :let [j (bit-shift-right index (inc i))]]
    [i j]))

(defn- initial-table [N]
  (vec (for [i (range (inc N))
             :let [Ni (if (< i N)
                        (bit-shift-left 1 (- N 1 i))
                        1)]]
         (vec (replicate Ni 0)))))

(defn- inc-symbol-index [table N sym-idx cnt]
  (reduce
   (fn [table indices] (update-in table indices + cnt))
   table (table-indices N sym-idx not)))

(defn- symbol-index-count [table N sym-idx]
  (loop [cnt (get-in table [N 0]) i (dec N)]
    (if (>= i 0)
      (let [j (bit-shift-right sym-idx (inc i))
            e (get-in table [i j])]
        (if (bit-test sym-idx i)
          (recur (- cnt e) (dec i))
          (recur e (dec i))))
      cnt)))

(defn- grow-table [N table sym-cnt]
  (let [new-N (inc N)]
    (reduce (fn [new-table sym-idx]
              (let [sym-cnt (symbol-index-count table N sym-idx)]
                (when (pos? sym-cnt)
                  (inc-symbol-index new-table new-N sym-idx sym-cnt))))
            (initial-table new-N) (range sym-cnt))))

(deftype CumulativeFrequencyTable [N symbols indices table]
  AdaptiveDiscreteDistribution
  (inc-symbol [o sym] (inc-symbol o sym 1))
  (inc-symbol [o sym cnt]
    (let [sym-cnt (count symbols)
          in-cft? (contains? indices sym)
          table-full? (== sym-cnt (bit-shift-left 1 N))
          new-N (if table-full? (inc N) N)
          symbols (if in-cft? symbols (conj symbols sym))
          indices (if in-cft? indices (assoc indices sym sym-cnt))
          sym-idx (indices sym)
          table (-> (if table-full? (grow-table N table sym-cnt) table)
                    (inc-symbol-index new-N sym-idx cnt))]
      (CumulativeFrequencyTable. new-N symbols indices table)))
  (symbol-count [o sym]
    (symbol-index-count table N (indices sym)))
  (total-count [o] (get-in table [N 0]))

  DiscreteDistribution
  (next-lower [o sym]
    (let [i (indices sym)]
      (when (pos? i) (symbols (dec i)))))
  (next-higher [o sym]
    (let [i (indices sym)]
      (when (< i (dec (count symbols))) (symbols (inc i)))))
  (cdf [o sym]
    (when-let [sym-idx (indices sym)]
      (/ (reduce (fn [cnt indices] (+ cnt (get-in table indices)))
              0 (table-indices N sym-idx identity))
         (total-count o))))
  (inverse-cdf [o interval]
    (letfn [(sym-idx [n]
              (loop [c 0 i (dec N) n n]
                (if (>= i 0)
                  (let [j (get-in table [i c])]
                    (if (< n j)
                      (recur (* 2 c) (dec i) n)
                      (recur (inc (* 2 c)) (dec i) (- n j))))
                  c)))]
      (let [[low high] (map (comp symbols sym-idx
                                  #(round (floor (* (total-count o) %))))
                            interval)]
        (when (= low high)
          low)))))

(defn cft
  ([] (cft 5))
  ([N] (CumulativeFrequencyTable. N [] {} (initial-table N))))
