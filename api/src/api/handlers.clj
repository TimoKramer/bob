(ns api.handlers
  (:require [clojure.java.io :as io]
            [taoensso.timbre :as log]
            [jsonista.core :as json]
            [api.helpers :as h])
  (:import (io.vertx.core Handler)
           (io.vertx.ext.web RoutingContext)
           (io.vertx.ext.web.api RequestParameters)))

(def api-spec-handler
  (reify Handler
    (handle [_ routing-ctx]
      (-> ^RoutingContext routing-ctx
          .response
          (.putHeader "content-type" "application/yaml")
          (.end (-> "api.yaml"
                    io/resource
                    slurp))))))

(def health-check-handler
  (h/->json-handler
    (constantly "Yes we can! \uD83D\uDD28 \uD83D\uDD28")))

(def pipeline-create-handler
  (h/->json-handler
    #(let [params   (.get ^RoutingContext % "parsedParameters")
           group    (h/get-path-param params "group")
           name     (h/get-path-param params "name")
           pipeline (-> ^RequestParameters params
                        .body
                        .toString
                        (json/read-value h/mapper))]
       (log/infof "Creating pipeline: %s %s %s" group name pipeline))))

(def failure-handler
  (h/->json-handler
    #(log/warnf (-> (.failure %)
                     (.getMessage)))))

(def pipeline-artifact-handler
  (reify Handler
    (handle [_ routing-ctx]
      (-> ^RoutingContext routing-ctx
          .response
          (.putHeader "content-type" "application/tar")
          (.sendFile "test.tar.gz")))))
