(ns project.testcase
  (:require [testify :refer [test-comment eval-as-use]]
            [clojure.test :refer [deftest is]]))

(test-comment
  (def a 2)
  (+ a 3)
  (is (= 5 *1)))

(deftest test-the-test
  (eval-as-use 'project.testcase))
