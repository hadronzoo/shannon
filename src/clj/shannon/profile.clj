(ns shannon.profile
  (:require [shannon.compressor :refer [compress decompress]]
            [taoensso.timbre :as timbre])
  (:gen-class))

(timbre/refer-timbre)

(defn -main [& args]
  (let [payload {:values (zipmap (range 100) (repeatedly 100 #(rand)))
                 :time "2014-01-01T00:00:00.000Z"
                 :use :profile} ;;(vec (range 100))
        compressed (compress payload)]
    (profile :info :Compression
             (dotimes [_ 10000]
               (pspy :compress (compress payload))
               (pspy :decompress (decompress compressed))))))
