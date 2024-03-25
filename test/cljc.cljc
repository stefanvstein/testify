(ns cljc
  (:require [testify :refer [test-comment eval-as-use]]
            [clojure.test :refer [is deftest]]))

(test-comment
 #?(:cljs (is false "clojurescript")
    :clj (is true "clojure")))

(deftest test-cljc
  (eval-as-use 'cljc))
