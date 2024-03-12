(ns repl-test
  (:require [repl-test.core :refer :all]))



(defmacro test-comment
  "A test to be run in a repl session. repl-test will run contained
  statement automitically, in a another namespace with everything in
  this namespace referred.
  It's like a comment, ignores body, yields nil"
  [& _])



(defn run-as-use
  "Run content in test-comment of ns in a tearoff namespace where ns is refered."
  [ns]
  (->> {:input-selector require-and-use
        :use-target? true
        :new-classpath? true
        :active-comment (var test-comment)}
       (repl-on ns)
       (dorun)))

(defn run-eval-all
  "Run content in test-comment of ns in a tearoff namespace in which all of ns is evaluated."
  [ns]
  (->> {:input-selector eval-all
        :new-classpath? true
        :active-comment (var test-comment)}
       (repl-on ns)
       dorun))

(defn run-in-ns
  "Run content in test-comment of ns"
  [ns]
  (->> {:run-all-cases? true
        :input-selector only-test
        :active-comment (var test-comment)}
       (repl-on ns)
       dorun))

(comment
  (run-as-use 'tryout)
  (run-eval-all "tryout")
  (run-in-ns 'tryout)

  #_no)
