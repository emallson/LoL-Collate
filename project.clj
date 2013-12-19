(defproject lol-collate "0.1"
  :description "Collects and collates data from the LoL REST API"
  :url "http://github.com/emallson/LoL-Collate"
  :license {:name "GNU General Public License Version 3"
            :url "http://www.gnu.org/licenses/gpl-3.0.txt"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [clj-http "0.7.8"]
                 [com.taoensso/carmine "2.4.0"]]
  :main ^:skip-aot lol-collate.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
