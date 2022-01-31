(defproject clojurecrazy "0.1.0-SNAPSHOT"
  :description "Clojure Crazy - A Blog and Sample Code"
  :url "https://github.com/dgr/clojurecrazy"
  :license {:name "multiple"
            :comments "The materials in this project are covered under multiple licenses. See the LICENSE file for more specific information."}
  :dependencies [[org.clojure/clojure "1.10.3"]
                 [org.clojure/core.logic "1.0.0"]
                 [criterium "0.4.6"]
                 [net.cgrand/xforms "0.19.2"]
                 [net.clojars.john/injest "0.1.0-beta.6"]]
  :repl-options {:init-ns user})
