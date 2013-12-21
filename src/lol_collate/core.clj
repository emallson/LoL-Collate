(ns lol-collate.core
  (:require [clojure.tools.cli :refer [parse-opts]]
            [lol-collate.collect :as collect]
            [atlanis.utils :refer [unless]])
  (:gen-class))

(def cli-opts
  [["-k" "--api-key KEY" "Riot API Key"
    :validate [#(not (nil? (re-matches #"([a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12})" %))) "Must be a valid api key"]]
   ["-e" "--explore NODE" "Explore starting from NODE"
    :parse-fn #(Integer/parseInt %)
    :validate [#(< 0 %) "Must be a positive number"]]])

(defn -main
  [& args]
  (let [cli-map (parse-opts args cli-opts)
        opts (:options cli-map)]
    
    (if (and (contains? opts :api-key)
             (nil? (:errors cli-map)))
      (do (reset! collect/api-key (:api-key opts))
          (when (contains? opts :explore)
            (collect/explore (:explore opts))))
      (do (unless (nil? (:errors cli-map))
                  (doseq [error (:errors cli-map)]
                    (println error)))
          (println "Usage: ")
          (println (:summary cli-map))))))
