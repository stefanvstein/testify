(defproject org.clojars.vstein/testify "0.4.1"
  :description "Avoid the hassle of restructuring comments into functions.
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
  :dependencies [[org.clojure/clojure "1.11.1"]]
  :url "https://github.com/stefanvstein/testify"
  :license {:name "Eclipse Public License - v 1.0"
            :url "http://www.eclipse.org/legal/epl-v10.html"
            :distribution :repo
            :comments "same as Clojure"}
  :repl-options {:init-ns testify})
