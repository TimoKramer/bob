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

(ns entities.resource-provider.core
  (:require [failjure.core :as f]
            [taoensso.timbre :as log]
            [entities.resource-provider.db :as db]))

(defn register-resource-provider
  "Registers a rersource provider with an unique name and an url supplied in a map."
  [db-conn data]
  (let [result (f/try* (db/register-resource-provider db-conn data))]
    (if (f/failed? result)
      (log/errorf "Could not register resource provider: %s" (f/message result))
      (do
        (log/infof "Registered resource provider at: %s" data)
        "Ok"))))

(defn un-register-resource-provider
  "Unregisters an resource provider by its name supplied in a map."
  [db-conn data]
  (f/try* (db/un-register-resource-provider db-conn data))
  (log/infof "Un-registered resource provider %s" name)
  "Ok")