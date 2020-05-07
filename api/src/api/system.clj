(ns api.system
  (:require [com.stuartsierra.component :as component]
            [taoensso.timbre :as log]
            [api.handlers :as h]
            [api.helpers :as hp])
  (:import (io.vertx.core Vertx
                          VertxOptions
                          Handler
                          Future)
           (io.vertx.ext.web.api.contract.openapi3 OpenAPI3RouterFactory)))

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
                            "PipelineDelete"
                            h/pipeline-delete-handler)
  (.addHandlerByOperationId route-factory
                            "PipelineStart"
                            h/pipeline-start-handler)
  (.addHandlerByOperationId route-factory
                            "PipelineStop"
                            h/pipeline-stop-handler)
  (.addHandlerByOperationId route-factory
                            "PipelineLogs"
                            h/pipeline-logs-handler)
  (.addHandlerByOperationId route-factory
                            "PipelineStatus"
                            h/pipeline-status-handler)
  (.addHandlerByOperationId route-factory
                            "PipelineArtifactFetch"
                            h/pipeline-artifact-handler)
  (.addHandlerByOperationId route-factory
                            "PipelineList"
                            h/pipeline-list-handler)
  (.addHandlerByOperationId route-factory
                            "ResourceProviderRegistration"
                            h/resource-provider-registration-handler)
  (.addFailureHandlerByOperationId route-factory
                                   "ResourceProviderRegistration"
                                   h/failure-handler)
  (.addHandlerByOperationId route-factory
                            "ResourceProviderDelete"
                            h/resource-provider-delete-handler)
  (.addHandlerByOperationId route-factory
                            "ResourceProviderList"
                            h/resource-provider-list-handler)
  (.addHandlerByOperationId route-factory
                            "ArtifactStoreRegistration"
                            h/artifact-store-registration-handler)
  (.addFailureHandlerByOperationId route-factory
                                   "ArtifactStoreRegistration"
                                   h/failure-handler)
  (.addHandlerByOperationId route-factory
                            "ArtifactStoreDelete"
                            h/artifact-store-delete-handler)
  (.addHandlerByOperationId route-factory
                            "ArtifactStoreList"
                            h/artifact-store-list-handler)

  (-> vertx
      .createHttpServer
      (.requestHandler (.getRouter route-factory))
      (.listen port host handler)))

(defprotocol IVerticle
  (verticle [this]))

(defrecord Verticle
  []
  component/Lifecycle
  (start [this]
    (let [vertx (Vertx/vertx (-> (VertxOptions.)
                                 (.setHAEnabled true)))
          verticle (hp/make-verticle {:on-start #(-> (hp/unit make-route-factory "/api.yaml" vertx)
                                                   (hp/then make-server 7777 "0.0.0.0" vertx)
                                                   (.onComplete (hp/->handler %)))
                                    :on-stop  #(constantly (println "yalla"))})]
      (.deployVerticle vertx verticle (hp/result))
      (assoc this :conn verticle)))
  (stop [this]
    (log/info "Stopping Verticle")
    (assoc this :conn nil))
  IVerticle
  (verticle [this]
    (:conn this)))

(def system-map
  (component/system-map
    :verticle (map->Verticle {})))

(defonce system nil)

(defn start
  []
  (alter-var-root #'system
                  (constantly (component/start system-map))))
