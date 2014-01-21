(ns shannon.type-registration
  (:require [shannon.coding-primitives :refer [Codeable encode decode]]
            [shannon.compatibility :refer [is-boolean? is-date? is-float?]]
            [shannon.polymorphic-coders :refer [default-coder register-type!
                                                list-coder vec-coder map-coder
                                                set-coder]]
            [shannon.base-coders :refer [nil-coder booleancoder symbol-coder
                                         date-coder adaptive-string-coder
                                         keyword-coder int64coder doublecoder]])
  (#+clj :use #+cljs :require-macros [shannon.register-type :as m]))

(def atom-pr 1.)
(def coll-pr 1.)

;; Standard atoms
(m/register-polymorphic-type! :nil, nil, :pr atom-pr, :coder nil-coder, :test nil?)

(m/register-polymorphic-type! :boolean,
                              #+clj java.lang.Boolean
                              #+cljs boolean,
                              :pr atom-pr, :coder booleancoder, :test is-boolean?)

(m/register-polymorphic-type! :symbol,
                              #+clj clojure.lang.Symbol
                              #+cljs cljs.core.Symbol,
                              :pr atom-pr, :coder symbol-coder, :test symbol?)

(m/register-polymorphic-type! :date,
                              #+clj [org.joda.time.DateTime java.util.Date]
                              #+cljs [goog.date.Date goog.date.DateTime js/Date],
                              :pr atom-pr, :coder date-coder, :test is-date?)

(m/register-polymorphic-type! :string,
                              #+clj java.lang.String,
                              #+cljs string,
                              :pr atom-pr, :coder adaptive-string-coder, :test string?)

(m/register-polymorphic-type! :keyword,
                              #+clj clojure.lang.Keyword,
                              #+cljs cljs.core.Keyword,
                              :pr atom-pr, :coder keyword-coder, :test keyword?)

#+clj
(do
  (m/register-polymorphic-type! :integer,
                                java.lang.Long,
                                :pr atom-pr, :coder int64coder, :test integer?)
  (m/register-polymorphic-type! :float,
                                java.lang.Double,
                                :pr atom-pr, :coder doublecoder, :test float?))

#+cljs
(do ;; JS can't directly dispatch on integer/float, so these must be handled manually
  (register-type! default-coder, :integer,
                  :pr atom-pr, :coder int64coder,  :test integer?)
  (register-type! default-coder, :float,
                  :pr atom-pr, :coder doublecoder, :test is-float?)
  (extend-protocol Codeable
    number
    (encode [o s] (encode default-coder s o (if (integer? o) :integer :float)))
    (decode [_ s] (decode default-coder s))))

;; Standard collections
(m/register-polymorphic-type! :list,
                              #+clj [clojure.lang.PersistentList,
                                     clojure.lang.PersistentList$EmptyList,
                                     clojure.lang.LazySeq],
                              #+cljs [cljs.core.List,
                                      cljs.core.EmptyList,
                                      cljs.core.LazySeq],
                              :pr coll-pr, :coder list-coder, :test list?)

(m/register-polymorphic-type! :vector,
                              #+clj clojure.lang.PersistentVector,
                              #+cljs cljs.core.PersistentVector,
                              :pr coll-pr, :coder vec-coder, :test vector?)

(m/register-polymorphic-type! :map,
                              #+clj [clojure.lang.PersistentArrayMap,
                                     clojure.lang.PersistentHashMap,
                                     clojure.lang.PersistentHashMap$TransientHashMap,
                                     clojure.lang.PersistentTreeMap],
                              #+cljs [cljs.core.PersistentArrayMap,
                                      cljs.core.PersistentHashMap,
                                      cljs.core.TransientHashMap,
                                      cljs.core.PersistentTreeMap],
                              :pr coll-pr, :coder map-coder, :test map?)

(m/register-polymorphic-type! :set,
                              #+clj [clojure.lang.PersistentHashSet,
                                     clojure.lang.PersistentHashSet$TransientHashSet,
                                     clojure.lang.PersistentTreeSet],
                              #+cljs [cljs.core.PersistentHashSet,
                                      cljs.core.TransientHashSet,
                                      cljs.core.PersistentTreeSet],
                              :pr coll-pr, :coder set-coder, :test set?)
