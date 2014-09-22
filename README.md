## datomic/schema について

### 必須の schema 項目

#### db/ident
db/ident は必須の値。 hoge.fuga/piyo のように書ける。hoge.fuga は namespace で、 db は予約されている。

#### db/valueType
db/valueType も必須。keyword で指定される。 以下の属性から選ぶ。  

{% blockquote %}
 :db.type/keyword - Value type for keywords. Keywords are used as names, and are interned for efficiency. Keywords map to the native interned-name type in languages that support them.

:db.type/string - Value type for strings.

:db.type/boolean - Boolean value type.

:db.type/long - Fixed integer value type. Same semantics as a Java long: 64 bits wide, two's complement binary representation.

:db.type/bigint - Value type for arbitrary precision integers. Maps to java.math.BigInteger on Java platforms.

:db.type/float - Floating point value type. Same semantics as a Java float: single-precision 32-bit IEEE 754 floating point.

:db.type/double - Floating point value type. Same semantics as a Java double: double-precision 64-bit IEEE 754 floating point.

:db.type/bigdec - Value type for arbitrary precision floating point numbers. Maps to java.math.BigDecimal on Java platforms.

:db.type/ref - Value type for references. All references from one entity to another are through attributes with this value type.

:db.type/instant - Value type for instants in time. Stored internally as a number of milliseconds since midnight, January 1, 1970 UTC. Maps to java.util.Date on Java platforms.

:db.type/uuid - Value type for UUIDs. Maps to java.util.UUID on Java platforms.

:db.type/uri - Value type for URIs. Maps to java.net.URI on Java platforms.

:db.type/bytes - Value type for small binary data. Maps to byte array on Java platforms.
{% endblockquote %}


#### db/cardinality
カーディナリティ。1 か N かを表現する。  

{% blockquote %}
:db.cardinality/one - the attribute is single valued, it associates a single value with an entity

:db.cardinality/many - the attribute is multi valued, it associates a set of values with an entity

Transactions can add or retract individual values for multi-valued attributes. 

{% endblockquote %}

### オプションの schema 項目について

{% blockquote %}
:db/doc specifies a documentation string.
:db/unique - specifies a uniqueness constraint for the values of an attribute. Setting an attribute :db/unique also implies :db/index. 
The values allowed for :db/unique are:
    :db.unique/value - the attribute value is unique to each entity; attempts to insert a duplicate value for a different entity id will fail
    :db.unique/identity - the attribute value is unique to each entity and "upsert" is enabled; attempts to insert a duplicate value for a temporary entity id will cause all attributes associated with that temporary id to be merged with the entity already in the database.
    :db/unique defaults to nil.
:db/index specifies a boolean value indicating that an index should be generated for this attribute. Defaults to false.
:db/fulltext specifies a boolean value indicating that an eventually consistent fulltext search index should be generated for the attribute. Defaults to false.
:db/isComponent specifies that an attribute whose type is :db.type/ref refers to a subcomponent of the entity to which the attribute is applied. When you retract an entity with
:db.fn/retractEntity, all subcomponents are also retracted. When you touch an entity, all its subcomponent entities are touched recursively. Defaults to nil.
:db/noHistory specifies a boolean value indicating whether past values of an attribute should not be retained. Defaults to false.
{% endblockquote %}


## スキーマ定義

{% blockquote %}
{:db/id #db/id[:db.part/db]
 :db/ident :person/name
 :db/valueType :db.type/string
 :db/cardinality :db.cardinality/one
 :db/doc "A person's name"
 :db.install/_attribute :db.part/db}
{% endblockquote %}
db.install の _attribute の _ は 逆方向の参照を使っていて、　db.part/db から 新しい参照への参照を作る？らしい。
day-of-datomic の例でも全部:db.install/_attribute :db.part/db だったので、今のところ決め打ちでもいいかもしれない。

## パーティション
についても書かれているが、様々なサンプルみても使ってないのでスルー。


(def db (d/db conn))
(d/q '[:find ?ident
:where [_ :db/ident ?ident]]
db)

## 実際にやってみる

データの追加

(defn dbschema []
  [(s/schema user
    (fields
     [username :string :indexed]
     [pwd :string "Hashed password string"]
     [email :string :indexed]
     [status :enum [:pending :active :inactive :cancelled]]
     [group :ref :many]))

   (s/schema group
    (fields
     [name :string]
     [permission :string :many]))])

## insert 成功
(d/transact (d/connect db-url)
[{:db/id #db/id[:db.part/user]
    :user/username "vimtaku"
    :user/pwd "hogeFuga1"
    :user/email "vimtaku@gmail.com"
    :user/status :user.status/pending
}
])




(d/q '[:find ?e
       :where [?e :user/username]
      ] db)

(d/q '[:find ?c :where [?c :user/pwd]] db)


(d/transact (d/connect db-url)
[{:db/id #db/id[:db.part/user]
    :user/username "vimtaku"
    :user/pwd "hogeFuga2"
    :user/email "vimtaku@gmail.com"
    :user/status :user.status/pending
}
])


(defn vimtaku []
    (def q-result (d/q '[:find ?e ?u ?p ?em ?s
                         :in $ ?name
                         :where
                             [?e :user/username ?name]
                             [?e :user/username ?u]
                             [?e :user/pwd ?p]
                             [?e :user/email ?em]
                             [?b :user/status ?_s]
                             [?_s :db/ident ?s]
                        ]
                       (d/db (d/connect db-url))
                       "vimtaku2"))
    q-result
)


(def ent (d/entity dbval (ffirst q-result)))

(d/transact (d/connect db-url)
[{:db/id #db/id[:db.part/user]
    :user/username "vimtaku"
    :user/status :user.status/active
}
])
これで upsert される.




(defn lookup [un]
    (def q-result (d/q '[:find ?e ?u ?p ?em ?s
                         :where
                             [?e :user/username ~un]
                             [?e :user/username ?u]
                             [?e :user/pwd ?p]
                             [?e :user/email ?em]
                             [?b :user/status ?_s]
                             [?_s :db/ident ?s]
                        ]
                       (d/db (d/connect db-url))))
    q-result
)

(defn lookup [un]
    (def q-result (d/q '[:find ?name ?p ?em ?s
                         :in $ ?name
                         :where
                             [?e :user/username ?name]
                             [?e :user/pwd ?p]
                             [?e :user/email ?em]
                             [?b :user/status ?_s]
                             [?_s :db/ident ?s]
                        ]
                       (d/db (d/connect db-url))
                       un))
    q-result
)
