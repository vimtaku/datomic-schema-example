(ns schema.setup
  (:use [datomic-schema.schema])
  (:require
            [compojure.core :refer :all]
            [compojure.handler :as handler]
            [compojure.route :as route]
            [datomic.api :as d]
            [datomic-schema.schema :as s]
            ))

(defonce db-url "datomic:mem://testdb")

(defn dbparts []
  [(s/part "app")])

(defn dbschema []
  [(s/schema user
    (fields
     [username :string :indexed :unique-identity]
     [pwd :string "Hashed password string"]
     [email :string :indexed]
     [status :enum [:pending :active :inactive :cancelled]]
     [group :ref :many]))

   (s/schema group
    (fields
     [name :string]
     [permission :string :many]))])

(defn setup-db [url]
  (d/create-database url)
  (d/transact
   (d/connect url)
   (concat
    (s/generate-parts d/tempid (dbparts))
    (s/generate-schema d/tempid (dbschema)))))
