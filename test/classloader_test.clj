(ns classloader-test
  (:require [clojure.test :refer [deftest is]]
            [testify :as t]
            [test-macros]))

(def cl nil)

(test-macros/use-test
 (is (not= cl (.getContextClassLoader (Thread/currentThread)))))

(test-macros/in-ns-test
 (is (= cl  (.getContextClassLoader (Thread/currentThread)))))

(deftest class-loader
  (with-redefs [cl (.getContextClassLoader (Thread/currentThread))]
    (t/eval-as-use 'classloader-test {:test-var  #'test-macros/use-test})
    (t/eval-in-ns 'classloader-test {:test-var #'test-macros/in-ns-test})))
