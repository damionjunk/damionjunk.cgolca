(defproject damionjunk.cgolca "0.1.0-SNAPSHOT"
  :description "ASCII Conway's Game of Life in CLJS/OM"
  :url "https://github.com/damionjunk/"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [org.clojure/clojurescript "0.0-3308"]
                 [org.clojure/core.async "0.1.346.0-17112a-alpha"]
                 [org.omcljs/om "0.8.8"]]
  :plugins [[lein-cljsbuild "1.0.6"]
            [lein-figwheel "0.3.3"]]
  :figwheel {
             :nrepl-port 7888
             }
  :cljsbuild {
              :builds [{:id           "dev"
                        :figwheel true
                        :source-paths ["src"]
                        :compiler     {:output-to     "resources/public/js/app.js"
                                       :output-dir    "resources/public/js/out/"
                                       :optimizations :none
                                       :source-map    true}}
                       {:id           "release"
                        :source-paths ["src"]
                        :compiler     {:main          main.core
                                       :output-to     "resources/public/js/app.js"
                                       :optimizations :advanced
                                       :pretty-print  false}}]})
