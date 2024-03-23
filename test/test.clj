(ns test
  (:require [clojure.test :refer [deftest is]]
            [repl-test :refer :all]
            [test-macros :refer :all]))

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
  (run-as-use 'test {:test-comment #'use-test})
  (run-eval-all 'test {:test-comment #'eval-test})
  (run-in-ns 'test {:test-comment #'in-ns-test}))
