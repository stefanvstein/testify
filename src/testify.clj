(ns testify
  "Avoid the hassle of restructuring comments into functions.
  Testify turns selected comments into automatically evaluated
  scripts, while remaining as comments embedded within the code.
  Evaluate these comments directly from within a test, or in any
  other way preferred. Rename the (comment) to (test-comment)
  and evaluate with (eval-in-ns 'your-namespace). Testify will
  find the test-comment and evaluate its content for you. The
  test-comment is an empty macro ignoring its body, just like
  comment, that Testify recognizes. Testify can easily be told
  to evaluate content of any other top level form, while
  test-comment is a default. Testify use levels of isolation.
  While eval-in-ns evaluates expressions in its namespace, like
  when evaluating comments manually, its sibling eval-as-use
  evaluates from within a temporary namespace, preventing
  pollution. This is more suitable for repeatable tests.
  Testify reads source code, and keeps track of where it is.
  Code should be highlighted when a test assertion fails, even
  though the assertion is in a comment. Testify is not a testing
  framework, but rather a pun on: to witness, reveal comment,
  display it in the repl."
  (:require [testify.impl :as t]))


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
     (->> (t/translate-classpath-option options)
          (merge {:input-selector t/require-and-use
                    :use-target? true
                    :new-classloader? true
                    :test-var #'test-comment})
          (assoc {} :options)
          (t/repl ns)
          (dorun)))))

(defn eval-all
  "Evaluate content of each test-comment of namespace ns in
  its own anonymous temporary namespace, in which all
  content of namespace ns is evaluated. Evaluation of the
  test-comment is peformed after all other content of ns
  has been evaluated. For options, see README"
  ([ns]
   (eval-all ns {}))
  ([ns options]
   (if (var? options)
     (eval-all ns {:test-var options})
     (->> (t/translate-classpath-option options)
          (merge {:input-selector t/eval-all
                  :new-classloader? true
                  :test-var #'test-comment})
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
     (->> (t/translate-classpath-option options)
          (merge {:run-all-cases? true
                  :input-selector t/only-test
                  :test-var #'test-comment})
          (assoc {} :options)
          (t/repl ns)
          dorun)))
  ([ns]
   (eval-in-ns ns {})))
