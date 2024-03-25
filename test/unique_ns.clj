(ns unique-ns
  (:require [testify :refer [eval-as-use]]
            [clojure.test :refer [deftest is]]))

(deftest shellfish
  (is (= (with-out-str (eval-as-use 'unique-ns 
                                    {:unique-ns #(symbol (str % "-as-myself"))}))
"(clojure.core/in-ns 'unique-ns-as-myself)
=> #namespace[unique-ns-as-myself]

(clojure.core/use 'clojure.core)
=> nil

(clojure.core/require
 '[testify :refer [eval-as-use]]
 '[clojure.test :refer [deftest is]])
=> nil

(clojure.core/use 'unique-ns)
=> nil

(clojure.core/remove-ns 'unique-ns-as-myself)
=> #namespace[unique-ns-as-myself]

")))
