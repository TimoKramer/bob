(ns api.core
  (:require [taoensso.timbre :as log]
            [api.system :as system])

  (:gen-class))

(log/set-level! :debug)

(defn -main
  [& _]
  (system/start))

(comment
  (require
    '(io.vertx.pgclient PgConnectOptions
                        PgPool)
    '(io.vertx.sqlclient PoolOptions))
  (-> (PgPool/pool "postgresql://bob:bob@localhost:5432/bob")
      (.query "SELECT 1 + 1 = 2")
      (.execute (reify Handler
                  (handle [_ future]
                    (let [f ^Future future]
                      (if (.succeeded f)
                        (log/infof "Postgres is up! %s" (str (first (iterator-seq (.iterator (.result f))))))
                        (do (log/errorf "Querying Postgres failed: %s" (.getMessage (.cause f)))
                            (throw (.cause f)))))))))
  )
