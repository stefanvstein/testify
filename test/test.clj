(ns test
  (:require [clojure.test :refer [deftest 
                                  is]]
            [testify :refer [eval-as-use
                             eval-all
                             eval-in-ns]]
            [test-macros :refer [use-test
                                 eval-test
                                 in-ns-test]]))

(def a (atom 0))

(use-test
 (is (= 11 (+ 5 6)))
 (is (= 0 @a))
 (swap! a inc))

(use-test
 (is (= 1 @a))
 (swap! a inc)
 (is (= 2 @a)))

(eval-test
  (is (= 0 @a))
  (swap! a inc)
  (is (= 1 @a)))

(eval-test
  (is (= 0 @a))
  (swap! a inc)
  (is (= 1 @a)))

(in-ns-test
  (is (= 2 @a))
  (swap! a inc)
  (is (= 3 @a)))

(in-ns-test
  (is (= 3 @a))
  (swap! a inc)
  (is (= 4 @a)))

(deftest tests
  (reset! a 0)
  (eval-as-use 'test {:test-comment #'use-test})
  (eval-all 'test {:test-comment #'eval-test})
  (eval-in-ns 'test {:test-comment #'in-ns-test}))
