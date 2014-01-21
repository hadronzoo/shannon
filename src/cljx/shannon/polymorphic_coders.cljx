(ns shannon.polymorphic-coders
  (:require [shannon.coding-primitives :refer [Codeable custom decode encode
                                               fixed-array transform
                                               variable-array]]
            [shannon.base-coders :refer [uint64coder]]))

(declare default-coder)

(deftype PolymorphicCoder [type-coder]
  Codeable
  (encode [_ scoder sym]
    (encode sym scoder))
  (decode [_ scoder]
    (decode default-coder scoder)))

(def polymorphic (PolymorphicCoder. default-coder))

(defn- type-directory->coder [directory]
  (let [dist (zipmap (keys directory)
                     (map :pr (vals directory)))
        flag (custom dist)]
    (reify Codeable
      (encode [_ scoder sym typehint]
        (encode flag scoder typehint)
        (encode (get-in directory [typehint :coder]) scoder sym))
      (encode [_ scoder sym]
        (let [specs (drop-while (fn [[k v]] (not ((v :test) sym)))
                                directory)
              [k spec] (first specs)]
          (encode flag scoder k)
          (encode (spec :coder) scoder sym)))
      (decode [_ scoder]
        (let [k (decode flag scoder)]
          (decode (get-in directory [k :coder]) scoder))))))

(defn- get-types-coder [type-directory types-coder]
  (or @types-coder
      (reset! types-coder (type-directory->coder @type-directory))))

(deftype RegisteredTypesCoder [type-directory types-coder]
  Codeable
  (encode [_ scoder sym typehint]
    (encode (get-types-coder type-directory types-coder) scoder sym typehint))
  (encode [_ scoder sym]
    (encode (get-types-coder type-directory types-coder) scoder sym))
  (decode [_ scoder]
    (decode (get-types-coder type-directory types-coder) scoder)))

(def default-coder (RegisteredTypesCoder. (atom nil) (atom nil)))

(defn register-type! [registry name
                      & {:keys [coder test pr] :or {pr 1}}]
  {:pre [(not (nil? (and registry name coder test)))]}
  (swap! (.-type-directory registry) assoc name {:pr pr
                                                :coder coder
                                                :test test} )
  (reset! (.-types-coder registry) nil))

(defn unregister-type! [registry name]
  (swap! (.-type-directory registry) dissoc name)
  (reset! (.-types-coder registry) nil))

(def coll-coder (variable-array polymorphic uint64coder))

(def list-coder (transform (variable-array polymorphic uint64coder)
                           identity #(into () (reverse %))))

(def vec-coder (transform (variable-array polymorphic uint64coder)
                          identity vec))

(def map-coder (transform (variable-array
                           (fixed-array [polymorphic polymorphic]) uint64coder)
                          identity #(into {} (map vec %))))

(def set-coder (transform (variable-array polymorphic uint64coder)
                          identity set))
