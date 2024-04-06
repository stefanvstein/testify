(ns testify
  "Testify turns code in your comments into automatically
  evaluated scripts, while still remaining embedded as
  comment within your code. You rename the comment to
  test-comment and evaluate (eval-in-ns 'your-namespace)
  to automate evaluation"
  (:require [testify.core :as t]))

(defmacro test-comment
  "Testify will find the top level test-comment and evaluate it
  automatically. The test-comment is in all means a comment, 
  except for its name. It ignores its body and yields nil"
  [& _])

(defn eval-as-use
  "Evaluate content of each test-comment of namespace ns in
  its own anonymous namespace where ns is referred to as use. 
  For options, see README"
  ([ns]
   (eval-as-use ns {}))
  ([ns options]
   (if (var? options)
     (eval-as-use ns {:test-var options})
     (->> (merge {:input-selector t/require-and-use
                  :use-target? true
                  :new-classpath? true
                  :test-var #'test-comment}
                 options)
          (assoc {} :options)
          (t/repl ns)
          (dorun)))))

(defn eval-all
  "Evaluate content of each test-comment of namespace ns in
  its own anonymous tear-off namespace, in which all
  content of namespace ns is evaluated. Evaluation of the
  test-comment is peformed after all other content of ns
  has been evaluated. For options, see README"
  ([ns]
   (eval-all ns {}))
  ([ns options]
   (if (var? options)
     (eval-all ns {:test-var options})
     (->> (merge {:input-selector t/eval-all
                  :new-classpath? true
                  :test-var #'test-comment}
                 options)
          (assoc {} :options)
          (t/repl ns)
          dorun))))

(defn eval-in-ns
  "Evaluate content in test-comments of namespace ns. This
  may alter the namespace in the same way as if the content
  of the test-comment was evaluated manually. For options,
  see README"
  ([ns options]
   (if (var? options)
     (eval-in-ns ns {:test-var options})
     (->> (merge {:run-all-cases? true
                  :input-selector t/only-test
                  :test-var #'test-comment}
                 options)
          (assoc {} :options)
          (t/repl ns)
          dorun)))
  ([ns]
   (eval-in-ns ns {})))
