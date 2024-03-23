(ns project.testcase
  (:require [repl-test :refer [test-comment run-as-use]]
            [clojure.test :refer [deftest is]]))

(test-comment
  (def a 2)
  (+ a 3)
  (is (= 5 *1)))

(deftest test-the-test
  (run-as-use 'project.testcase))
