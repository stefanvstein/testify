(defproject org.clojars.vstein/testify "0.3.0"
  :description "Your comment became a usable snippet of code! Testify turns these comments into automatically evaluated scripts, while still remaining embedded as a comment within your code. You rename the comment to test-comment and evaluate (eval-in-ns 'your-namespace) to automate evaluation."
  :dependencies [[org.clojure/clojure "1.11.1"]]
  :url "https://github.com/stefanvstein/testify"
  :license {:name "Eclipse Public License - v 1.0"
            :url "http://www.eclipse.org/legal/epl-v10.html"
            :distribution :repo
            :comments "same as Clojure"}
  :repl-options {:init-ns testify})
