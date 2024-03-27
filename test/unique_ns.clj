(ns unique-ns
  (:require [testify :refer [eval-as-use]]
            [clojure.test :refer [deftest is]]))

(deftest shellfish
  (is (= 'unique-ns-as-myself
       (-> (with-out-str (eval-as-use 'unique-ns
                                      {:unique-ns #(symbol (str % "-as-myself"))}))
           (read-string)
           (second)
           (second)))))
