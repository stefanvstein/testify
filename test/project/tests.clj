(ns project.tests
  (:require [testify :refer [eval-as-use]]
            [clojure.test :refer [deftest is]]))
			
(defmacro unit-test [& _])

(unit-test 
  (def a 2)
  (+ a 3)
  (is (= 5 *1)))

(deftest test-the-test
  (eval-as-use 'project.tests {:test-var #'unit-test}))

