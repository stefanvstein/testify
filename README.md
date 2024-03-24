# Testify
## Evaluate Clojure comments automatically

<img align="right" src="robby.jpg" width="150" height="150">

Your `comment` became an usable snippets of code. 
**Testify** turns these comments into automatically evaluated scripts, while remainig embedded.
You rename the `comment` to `test-comment`, and evaluate `(eval-in-ns 'your-namespace)` to automate re-evaluation.

Perhaps you want to evaluate these comments as example based tests.

## How
 
You are probably used to evaluate code manually within comment expressions, Rich comments. 

*This is very similar to a writing in a repl, but you select and evaluate an expression is a source code file rather than writing the expression up front. These expressions are hidden behind comments since they are expressions to interact with the system, rather than being part of the system. This is very popular development method in lisps, like Clojure.*

You have comment like this in you source files:

```clojure
(comment 
  (def a 2)
  (+ a 3))
```

It's also likely that you use comment expressions to do some administrative tasks, like poking in a database or similar.

With Testify you automate evaluation of similar comment expressions, named test-comment rather than comment. The test-comment macro is equal to the comment macro, which both ignore its body. As soon you have a comment expression which you want to evaluate automatically, perhaps as a test, you simply rename the comment to test-comment.

```clojure
(ns project.testcase
  (:require [testify :refer [test-comment eval-in-ns]]))

;; Test comments are just like comments, macros that ignore body  
(test-comment 
  (def a 2)
  (+ a 3))
  
(comment 
  ;; space for more experimental and administrative tasks,
  ;; like evaluating test-comments
  (eval-in-ns 'project.testcase))
```
You evaluate content of top level test-comment expressions by supplying the namespace to one of the three functions `eval-as-use`, `eval-all` or `eval-in-ns`. 

Note that only the content of top level test-comments in the source file will be evaluated. Nested test-comments will just be like ordinary comments, ignored. Other parts of the the source file will be considered before evaluating the test-comments, slightly different depending on evaluation method. The test-comments are thereafter evaluated from top to bottom as they appear. 

Every step and its result will be printed to \*out\*:
```
(clojure.core/in-ns 'project.testcase)
=> #namespace[project.testcase]

(def a 2)
=> #'project.testcase/a

(+ a 3)
=> 5
```
There are different evaluations functions, `eval-in-ns`, `eval-as-use` and `eval-all`, behaving slightly different:

`eval-in-ns` evaluates the content of the test-comments in its already existing namespace. This is pretty much the same as evaluating step by step manually as we usually do, but not always that great for automated testing. The namespace remains, possibly altered, afterwards. 

`eval-as-use` evaluates content of each test-comment in a new tear-off namespace and refer to the current namespace as referring by use, as depicted blow. All public functions are available. The tear-off namespace is deleted after each test-comment

`eval-all` evaluates all forms in namespace in another tear-off namespace. All vars are available, including private, except that they belong to the new temporary tear-off namespace. The tear-off is removed after each test-comment. The whole namespace is evaluated before test-comments are evaluated. The test-comment is simply a comment during initial evaluation.


Since these tests are automated, any exception thrown will stop the process. All remaining test-comments will be ignored up on a thrown exception.

A test-case can easily use clojure.test/is to verify facts along the way, and the above run functions can be called withing a deftest.

```clojure
(ns project.testcase
  (:require [testify :refer [test-comment eval-as-use]]
            [clojure.test :refer [deftest is]]))
  
(test-comment 
  (def a 2)
  (+ a 3)
  (is (= 5 *1)))

;; A unit test is better evaluated with eval-as-use
;; since it uses a tear-off namespace to prevent
;; altering the target. (def a 2) above is placed
;; in an anonymous tear-off namespace.
 
(deftest test-the-test
  (eval-as-use 'project.testcase))
```
Automated evaluation and their result is printed to \*out\*, including preparation like evaluating namespace. It should be easy to understand automated evaluation. Note tear-off namespace `project.testcase-9379`:

```
(clojure.core/in-ns 'project.testcase-9379)
=> #namespace[project.testcase-9379]

(clojure.core/use 'clojure.core)
=> nil

(clojure.core/require
 '[testify :refer [test-comment eval-as-use]]
 '[clojure.test :refer [deftest is]])
=> nil

(clojure.core/use 'project.testcase)
=> nil

(def a 2)
=> #'project.testcase-9379/a

(+ a 3)
=> 5

(is (= 5 *1))
=> true

(clojure.core/remove-ns 'project.testcase-9379)
=> #namespace[project.testcase-9379]
```

The `eval-as-use` and `eval-all` alternatives uses an isolated classloader, discarded after the run. The tools expects files with a leading ns form, but translates it to a in-ns, followed by individual requirements. The resulting namespace is not considered a loaded lib by Clojure.

This tool is heavily influenced by Cognitects Transcriptor library, which evaluates repl files in a similar fashion, which is not regular clojure source files, that your favorite dev environment already understand.

# Options

The eval- functions takes a optional map of options: 

`:test-comment` is a var of the macro used as test comment default value `#'testify/test-comment`. It can be changed to make selective runs in the same namespace. 

*Note that `eval-all` will evaluate the whole file representing the name space into a new anonymous namespace. If the optional test comment macro is defined here, it will most likely not match that when defined in test-comment option. Just put additional test-comment definitions in another namespace when using `eval-all`.* 

`:new-classpath?` is a boolean telling whether each test-comment should run in a new class-loader, so that locally defined types are omitted after each test comment. This is set for `eval-as-use` and `eval-all`.

`:keep-ns-on-exception?` is a boolean that when set omits removing any tear-off name space on thrown exception. Can be used to investigate circumstances

The other options `:execute-case`, `:current-case`, `:case-executed?`, `:forms`, `:there-is-more-form`, `:context` and `:remove-ns` are used internally and is subject to change

# License
Eclipse Public License, same as Clojure. https://www.eclipse.org/legal/epl-v10.html

