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
    #(let [params (.get ^RoutingContext % "parsedParameters")
           group (h/get-path-param params "group")
           name (h/get-path-param params "name")
           pipeline (-> ^RequestParameters params
                        .body
                        .toString
                        (json/read-value h/mapper))]
       (h/respond {:message  "Creating pipeline"
                   :group    group
                   :name     name
                   :pipeline pipeline}))))

(def pipeline-delete-handler
  (h/->json-handler
    #(let [params (.get ^RoutingContext % "parsedParameters")
           group (h/get-path-param params "group")
           name (h/get-path-param params "name")]
       (h/respond {:message "Deleting pipeline"
                   :group   group
                   :name    name}))))

(def pipeline-start-handler
  (h/->json-handler
    #(let [params (.get ^RoutingContext % "parsedParameters")
           group (h/get-path-param params "group")
           name (h/get-path-param params "name")]
       (h/respond {:message "Starting Pipeline"
                   :group   group
                   :name    name}))))

(def pipeline-stop-handler
  (h/->json-handler
    #(let [params (.get ^RoutingContext % "parsedParameters")
           _ (log/debugf "Stopping Pipeline with params: %s" (str params))
           group (h/get-path-param params "group")
           name (h/get-path-param params "name")
           number (h/get-path-param params "number" :as :int)]
       (h/respond {:message "Starting Pipeline"
                   :group   group
                   :name    name
                   :number  number}))))

; TODO optional arguments
(def pipeline-logs-handler
  (h/->json-handler
    #(let [params (.get ^RoutingContext % "parsedParameters")
           group (h/get-path-param params "group")
           name (h/get-path-param params "name")
           number (h/get-path-param params "number" :as :int)
           offset (h/get-path-param params "offset" :as :int)
           lines (h/get-path-param params "lines" :as :int)]
       (h/respond {:message "Starting Pipeline"
                   :group   group
                   :name    name
                   :number  number
                   :offset  offset
                   :lines   lines}))))

(def pipeline-status-handler
  (h/->json-handler
    #(let [params (.get ^RoutingContext % "parsedParameters")
           _ (log/debugf "Stopping Pipeline with params: %s" (str params))
           group (h/get-path-param params "group")
           name (h/get-path-param params "name")
           number (h/get-path-param params "number" :as :int)]
       (h/respond {:message "Pipeline Status"
                   :group   group
                   :name    name
                   :number  number}))))

(def pipeline-artifact-handler
  (reify Handler
    (handle [_ routing-ctx]
      (-> ^RoutingContext routing-ctx
          .response
          (.putHeader "content-type" "application/tar")
          (.sendFile "test.tar.gz")))))

; TODO optional parameters
(def pipeline-list-handler
  (h/->json-handler
    (constantly "Listing Pipelines")))

; TODO $.attrs: is missing but it is required
(def resource-provider-registration-handler
  (h/->json-handler
    #(let [params (.get ^RoutingContext % "parsedParameters")
           name (h/get-path-param params "name")]
       (h/respond {:message "Registering Resource Provider"
                   :name    name}))))

(def resource-provider-delete-handler
  (h/->json-handler
    #(let [params (.get ^RoutingContext % "parsedParameters")
           name (h/get-path-param params "name")]
       (h/respond {:message "Deleting Resource Provider"
                   :name    name}))))

(def resource-provider-list-handler
  (h/->json-handler
    #(let [params (.get ^RoutingContext % "parsedParameters")]
       (h/respond {:message "Listing Resource Providers"}))))

; TODO $.attrs: is missing but it is required
(def artifact-store-registration-handler
  (h/->json-handler
    #(let [params (.get ^RoutingContext % "parsedParameters")
           name (h/get-path-param params "name")]
       (h/respond {:message "Registering Artifact Store"
                   :name    name}))))

(def artifact-store-delete-handler
  (h/->json-handler
    #(let [params (.get ^RoutingContext % "parsedParameters")
           name (h/get-path-param params "name")]
       (h/respond {:message "Deleting Artifact Store"
                   :name    name}))))

(def artifact-store-list-handler
  (h/->json-handler
    #(let [params (.get ^RoutingContext % "parsedParameters")]
       (h/respond {:message "Listing Artifact Stores"}))))

(def failure-handler
  (h/->json-handler
    #(h/fail (-> (.failure %)
                 (.getMessage)))
    400))
