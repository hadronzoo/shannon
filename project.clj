(defproject com.joshuagriffith/shannon "0.2.0-SNAPSHOT"
  :description "Arithmetic coding library for Clojure and Clojurescript"
  :url "https://github.com/hadronzoo/shannon"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/math.numeric-tower "0.0.4"]
                 [clj-time "0.6.0"]
                 [com.github.jinahya/bit-io "1.1"]
                 [org.clojure/clojurescript "0.0-2138" :scope "provided"]
                 [com.cemerick/clojurescript.test "0.2.0"]
                 [com.taoensso/timbre "3.0.0-RC4"]]
  :source-paths ["src/clj" "target/src/clj"]
  :test-paths ["test/clj" "target/test/clj"]

  :main shannon.profile
  :profiles {:uberjar {:aot :all}}

  :hooks [cljx.hooks leiningen.cljsbuild]

  :plugins [[lein-cljsbuild "1.0.1"]
            [com.keminglabs/cljx "0.3.2"]
            [com.cemerick/clojurescript.test "0.2.0"]]

  :cljx {:builds [{:source-paths ["src/cljx"]
                   :output-path "target/src/clj"
                   :rules :clj}

                  {:source-paths ["src/cljx"]
                   :output-path "target/src/cljs"
                   :rules :cljs}

                  {:source-paths ["test/cljx"]
                   :output-path "target/test/clj"
                   :rules :clj}

                  {:source-paths ["test/cljx"]
                   :output-path "target/test/cljs"
                   :rules :cljs}]}

  :cljsbuild {:builds {:test
                       {:source-paths ["target/src/cljs" "target/test/cljs"]
                        :compiler {:output-to "resources/private/js/unit-test.js"
                                   :optimizations :whitespace
                                   :pretty-print true}}}

              :test-commands {"unit" ["phantomjs" :runner
                                      "resources/private/js/unit-test.js"]}})
