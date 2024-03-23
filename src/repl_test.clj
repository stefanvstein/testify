(ns repl-test
  (:require [repl-test.core :as t]))



(defmacro test-comment
  "A test to be run in a repl session. repl-test will run contained
  statement automitically, in a another namespace with everything in
  this namespace referred.
  It's like a comment, ignores body, yields nil"
  [& _])



(defn run-as-use
  "Run content in test-comment of ns in a tearoff namespace where ns is refered."
  ([ns] 
   (run-as-use ns {}))
  ([ns options]
   (->> (merge {:input-selector t/require-and-use
                :use-target? true
                :new-classpath? true
                :test-comment (var test-comment)}
               options)
        (t/repl ns)
        (dorun))))

(defn run-eval-all
  "Run content in test-comment of ns in a tearoff namespace in which all of ns is evaluated."
  ([ns]
   (run-eval-all ns {}))
  ([ns options]
   (->> (merge {:input-selector t/eval-all
                :new-classpath? true
                :test-comment (var test-comment)}
               options)
        (t/repl ns)
        dorun)))

(defn run-in-ns
  "Run content in test-comment of ns"
  ([ns options]
   (->> (merge {:run-all-cases? true
                :input-selector t/only-test
                :test-comment (var test-comment)}
               options)
        (t/repl ns)
        dorun))
  ([ns]
   (run-in-ns ns {})))


