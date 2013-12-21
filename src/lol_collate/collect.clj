(ns lol-collate.collect
  (:require [clj-http.client :as client]
            [atlanis.utils :refer [now unless]]
            [clojure.set :refer [union]]
            [taoensso.carmine :as car :refer [wcar]]))

(def conn {:pool {} :spec {:host "127.0.0.1" :port 6379}})
(defmacro wcar* [& body] `(car/wcar conn ~@body))

(def api-root "https://prod.api.pvp.net/api/lol/na/v1.1/")
(def api-key (atom nil))

(def resolution 43200) ; update a summoner no more than once every 12 hours
(def call-delay 1200) ; delay between API calls

(defn make-url
  "Generates a URL from the given parameters"
  [type id]
  (case type
    :summoner (str api-root "summoner/" id)
    :recent-games (str api-root "game/by-summoner/" id "/recent")
    :name (str api-root "summoner/" id "/name")
    :champion (str api-root "champion")))

(defn get-from-api
  "Wrapper for client/get to ease data collection."
  [type id & {:keys [query-params]}]
  (:body (client/get (make-url type id)
                     {:query-params (merge {:api_key @api-key} query-params)
                      :as :json})))

(defn get-fellow-summoners
  "Gets a list of fellow summoners from a list of games."
  [games]
  (reduce #(assoc %1 %2 (inc (%1 %2 0))) {} (map :summonerId (flatten (map :fellowPlayers (:games games))))))

(defn map-games
  [games]
  (let [sid (:summonerId games)]
    (apply hash-map (flatten (map #(list (:gameId %) (assoc % :summonerId sid)) (:games games))))))

(defn write-games
  "Writes a game map to redis. The values are stored as strings and are not
  currently used."
  [game-map]
  (wcar* (car/hmset* "games" game-map)))

(defn write-summoner
  [id fellows game-ids]
  (wcar* (car/sadd "summoner-ids" id)
         (apply car/zadd "tempfellows" (flatten (map #(list (second %) (first %)) fellows)))
         (car/zunionstore* (str "fellows:" id) [(str "fellows:" id) "tempfellows"])
         (car/del "tempfellows")
         (apply car/sadd (str "games:" id) game-ids)))

(defn explore
  [start]
  (loop [games (get-from-api :recent-games start)
         queue (keys (get-fellow-summoners games))]
    (let [current (:summonerId games)
          fellows (get-fellow-summoners games)
          game-map (map-games games)]
      (when games
        (write-games game-map)
        (write-summoner current fellows (keys game-map))
        (Thread/sleep call-delay))
      (let [next-summoner (first queue)
            lock-key (str "lock:" next-summoner)]
        (when next-summoner
          (if (= (wcar* (car/exists lock-key)) 0)
            (do (wcar* (car/setex lock-key resolution 1))
                (recur (get-from-api :recent-games next-summoner)
                       (take 1000 (union (rest queue) (keys fellows)))))
            (recur nil (take 1000 (union (rest queue) (keys fellows))))))))))
