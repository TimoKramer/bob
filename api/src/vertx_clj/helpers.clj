(ns vertx-clj.helpers
  (:require [jsonista.core :as json])
  (:import (java.util.function Function)
           (io.vertx.core Verticle
                          AbstractVerticle
                          Promise
                          AsyncResult
                          Future
                          Handler)
           (io.vertx.ext.web RoutingContext)
           (io.vertx.ext.web.api RequestParameters)))

(def mapper
  (json/object-mapper
    {:decode-key-fn true}))

(defn ->json-handler
  [action-fn]
  (reify Handler
    (handle [_ routing-ctx]
      (-> ^RoutingContext routing-ctx
          .response
          (.putHeader "content-type" "application/json")
          (.end (json/write-value-as-string
                  {:message (or (action-fn routing-ctx)
                                "Ok")}))))))

(defn get-path-param
  [^RequestParameters params ^String name & {:keys [as]}]
  (let [param (.pathParameter params name)]
    (case as
      :int (.getInteger param)
      (.getString param))))

(defn make-verticle
  ^Verticle
  [{:keys [on-start on-stop]}]
  (proxy [AbstractVerticle] []
    (start [promise]
      (when on-start
        (on-start promise)))

    (stop [promise]
      (when on-stop
        (on-stop promise)))))

(defn ->handler
  [^Promise p]
  (reify Handler
    (handle [_ result]
      (let [r ^AsyncResult result]
        (if (.succeeded r)
          (.complete p r)
          (.fail p (.cause r)))))))

(defn unit
  ^Future
  [f & args]
  (let [p (Promise/promise)]
    (apply f (conj (vec args)
                   (->handler p)))
    (.future p)))

(defn then
  ^Future
  [^Future prev-future next-fn & args]
  (let [f (reify Function
            (apply [_ future]
              (let [f ^Future future]
                (if (.succeeded f)
                  (apply unit next-fn (conj (vec args)
                                            (.result f)))
                  (throw (.cause f))))))]
    (.compose prev-future f)))
