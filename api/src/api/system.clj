(ns api.system
  (:require [com.stuartsierra.component :as component]
            [taoensso.timbre :as log]
            [api.helpers :as hp]
            [api.vertx :as vertx]))

(defprotocol IServer
  (vertx [this]))

(defrecord Server
  [vertx]
  component/Lifecycle
  (start [this]
    (let [vertx (vertx/make-vertx)
          verticle (vertx/make-verticle {:on-start #(-> (hp/unit vertx/make-route-factory "/api.yaml" vertx)
                                                   (hp/then vertx/make-server 7777 "0.0.0.0" vertx)
                                                   (.onComplete (hp/->handler %)))})]
      (.deployVerticle vertx verticle (hp/result))
      (assoc this :vertx vertx)))
  (stop [this]
    this
    (do (log/infof "Stopping Vertx %s" (str (type (:vertx this))))
        (.close vertx)
        (assoc this :vertx nil)))
  IServer
  (vertx [this]
    (:vertx [this])))

(def system-map
  (component/system-map
    :vertx (map->Server {})))

(defonce system nil)

(defn start
  []
  (alter-var-root #'system
                  (constantly (component/start system-map))))

(defn stop
  []
  (alter-var-root #'system
                  #(when %
                     (component/stop %))))

(defn reset
  []
  (stop)
  (start))

(comment
  (constantly (println "yalla"))
  (start)
  (stop)
  (:vertx system)
  (reset))
