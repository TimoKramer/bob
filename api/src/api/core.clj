(ns api.core
  (:require [taoensso.timbre :as log]
            [api.handlers :as h]
            [api.helpers :as hp])
  (:import (io.vertx.core Vertx
                          VertxOptions Handler)
           (io.vertx.ext.web.api.contract.openapi3 OpenAPI3RouterFactory))
  (:gen-class))

(log/set-level! :debug)

(defn make-route-factory
  [api-spec vertx handler]
  (OpenAPI3RouterFactory/create vertx api-spec handler))

(defn make-server
  [port host ^Vertx vertx ^OpenAPI3RouterFactory route-factory handler]
  (.addHandlerByOperationId route-factory
                            "HealthCheck"
                            h/health-check-handler)
  (.addHandlerByOperationId route-factory
                            "GetApiSpec"
                            h/api-spec-handler)
  (.addHandlerByOperationId route-factory
                            "PipelineCreate"
                            h/pipeline-create-handler)
  (.addFailureHandlerByOperationId route-factory
                                   "PipelineCreate"
                                   h/failure-handler)
  (.addHandlerByOperationId route-factory
                            "PipelineArtifactFetch"
                            h/pipeline-artifact-handler)
  (.addHandlerByOperationId route-factory
                            "PipelineList"
                            h/pipeline-list-handler)

  (-> vertx
      .createHttpServer
      (.requestHandler (.getRouter route-factory))
      (.listen port host handler)))

(defn -main
  [& _]
  (let [vertx  (Vertx/vertx (-> (VertxOptions.)
                                (.setHAEnabled true)))
        server (hp/make-verticle {:on-start #(-> (hp/unit make-route-factory "/api.yaml" vertx)
                                                 (hp/then make-server 7777 "0.0.0.0" vertx)
                                                 (.onComplete (hp/->handler %)))
                                  :on-stop  #(constantly (println "yalla"))})]
    (.deployVerticle vertx server (reify Handler
                                    (handle [_ _]
                                      (log/info "Bob is up!"))))))
