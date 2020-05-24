;   This file is part of Bob.
;
;   Bob is free software: you can redistribute it and/or modify
;   it under the terms of the GNU Affero General Public License as published by
;   the Free Software Foundation, either version 3 of the License, or
;   (at your option) any later version.
;
;   Bob is distributed in the hope that it will be useful,
;   but WITHOUT ANY WARRANTY; without even the implied warranty of
;   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
;   GNU Affero General Public License for more details.
;
;   You should have received a copy of the GNU Affero General Public License
;   along with Bob. If not, see <http://www.gnu.org/licenses/>.

(ns entities.util
  (:require [com.stuartsierra.component :as component]
            [next.jdbc :as jdbc]
            [next.jdbc.result-set :as rs]
            [entities.system :as sys]))

(defn with-db
  [test-fn]
  (let [url "jdbc:postgresql://localhost:5433/bob-test?user=bob&password=bob"
        db  (sys/map->Database {:jdbc-url           url
                                :connection-timeout 5000})
        com (component/start db)]
    (test-fn (sys/db-connection com))
    (component/stop com)))

(defn sql-exec!
  [db sql]
  (jdbc/execute! db [sql] {:builder-fn rs/as-unqualified-lower-maps}))
