(ns shannon.switch
  (:require [shannon.coding-primitives :refer [Codeable encode decode custom]]))

(defmacro switch [options]
  (let [testable-options (dissoc options :else)
        else-option (:else options)
        coders (gensym "coders")
        flag (gensym "flag")
        scoder (gensym "scoder")
        sym (gensym "sym")
        this (gensym "this")
        typehint (gensym "typehint")]
    `(let [~coders ~(zipmap (keys options) (map :coder (vals options)))
           ~flag (custom ~(zipmap (keys options)
                                               (map #(or (:pr %) 1) (vals options))))]
       (reify Codeable
         (~'encode [~this ~scoder ~sym ~typehint]
           (do (encode ~flag ~scoder ~typehint)
               (encode (~coders ~typehint) ~scoder ~sym)))

         (~'encode [~this ~scoder ~sym]
           (cond
            ~@(mapcat (fn [[k v]]
                        `((~(:test v) ~sym)
                          (encode ~this ~scoder ~sym ~k)))
                      testable-options)
            :else
            ~(if else-option
               `(do (encode ~flag ~scoder :else)
                    (encode ~(:coder else-option) ~scoder ~sym))
               'nil)))

         (~'decode [~this ~scoder]
           (let [k# (decode ~flag ~scoder)]
             (decode (or (~coders k#) ~(else-option :coder)) ~scoder)))))))
