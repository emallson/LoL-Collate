(ns lol-collate.collect-test
  (:require [clojure.test :refer :all]
            [lol-collate.collect :refer :all]
            [taoensso.carmine :as car]))

(deftest test-make-url
  (is (= (make-url :champion nil) "https://prod.api.pvp.net/api/lol/na/v1.1/champion"))
  (is (= (make-url :summoner 1) "https://prod.api.pvp.net/api/lol/na/v1.1/summoner/1"))
  (is (= (make-url :name "1,2") "https://prod.api.pvp.net/api/lol/na/v1.1/summoner/1,2/name"))
  (is (= (make-url :recent-games 1) "https://prod.api.pvp.net/api/lol/na/v1.1/game/by-summoner/1/recent")))

(def test-data {:games [{:fellowPlayers [{:summonerId 1}
                                       {:summonerId 2}
                                       {:summonerId 3}]
                         :gameId 1}
                      {:fellowPlayers [{:summonerId 2}
                                       {:summonerId 3}
                                       {:summonerId 4}]
                       :gameId 2}]
                :summonerId 13})

(deftest test-get-fellow-summoners
  (is (= (get-fellow-summoners test-data) {1 1, 2 2, 3 2, 4 1}))
  (is (= (get-fellow-summoners nil) {})))

(deftest test-map-games
  (is (= (keys (map-games test-data)) [1 2]))
  (is (= (map-games nil) {})))

(deftest test-write-games
  (let [game-map (map-games test-data)]
    (write-games game-map)
    (let [games (wcar* (car/hmget "games" 1 2))]
      (is (= (first games) (get game-map 1)))
      (is (= (second games) (get game-map 2))))))

(deftest test-write-summoner
  (let [id 13
        fellows (get-fellow-summoners test-data)
        games (keys (map-games test-data))]
    (write-summoner id fellows games)
    (write-summoner id (assoc fellows 7 1) (conj games 3))
    (let [fellowskey (str "fellows:" id)
          gameskey (str "games:" id)
          [fellow-zet games-set] (wcar* (car/zrange fellowskey 0 -1 :withscores)
                                        (car/smembers gameskey))]
      (is (= (wcar* (car/sismember "summoner-ids" 13)) 1))
      (is (= (apply hash-map fellow-zet) {"7" "1" "1" "2" "2" "4" "3" "4" "4" "2"}) "Check that each fellow is appropriately scored")
      (is (= games-set ["1" "2" "3"])))))

(defn fixture-redis-namespace
  [f]
  (wcar* (car/select 13))
  (f))

(defn fixture-redis-clean
  [f]
  (wcar* (car/flushdb))
  (f)
  (wcar* (car/flushdb)))

(use-fixtures :once fixture-redis-namespace)
(use-fixtures :each fixture-redis-clean)
