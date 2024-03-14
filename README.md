# Repl Test
**Running Clojure code comments, possibly in an anonymous namespace**

You are probably used to evaluate code manually within comment expressions, Rich comments. 
```clojure
(comment 
  (def a 2)
  (+ a 3))
```

You want to elaborate with code by evaluating it, step by step, as you program. It's also likely that you use comment expressions to do some administrative tasks, like poking in a database or similar.

With repl-test you automate evaluation of similar comment expression, named test-comment rather than comment. The test-comment macro is equal to the comment macro, which both simply ignore its body. As soon you have a comment expression which you want to evaluate automatically, perhaps as a test, but not your administrative routines, you simply rename the comment to test-comment.

```clojure
(ns project.testcase
  (:require [repl-test :refer [test-comment]]))
  
(test-comment 
  (def a 2)
  (+ a 3))
  
(comment 
  ;;space for more experimental and administrative tasks
  )
```

You run content of test-comment expressions with repl-test by supplying the namespace to one of the three functions `run-as-use`, `run-eval-all` or `run-in-ns`. Every step and its result will be printed to *out*   

```clojure
(ns project.testcase
  (:require [repl-test :refer [run-as-use]]))
  
(run-as-use 'project.testcase)
```
`run-as-use` runs content of each test-comment in a new tear-off namespace and refer to the current namespace as referring it as use. All public functions are available. The tear-off namespace is deleted after each test-comment

`run-eval-all` evaluates all forms in namespace in another tear-off namespace. All privates are available, except that they belong to the temporary tear-off namespace. The tear-off is removed after each test-comment. The whole namespace is evaluated before test-comments are evaluated. The test-comment is simply a comment during initial evaluation.

`run-in-ns` runs in the already existing name space, not in a tear-off. The namespace remains, possibly altered, afterwards. This is pretty much the same as evaluating step by step manually. 

Since these tests are automated, any exception thrown will stop the process. All remaining test-comments will be ignored on a thrown exception.

A test-case can easily use clojure.test/is to verify facts along the way, and the above run functions can be called withing a deftest.

```clojure
(ns project.testcase
  (:require [repl-test :refer [test-comment run-as-use]]
            [clojure.test :refer [deftest is]]))
  
(test-comment 
  (def a 2)
  (+ a 3)
  (is (= 5 *1)))
	
(deftest test-the-test
  (run-as-use 'project.testcase))
```
Automated evaluation and their result is printed to *out*, including preparation like evaluating namespace. It should be easy to understand automated evaluation.

The run-as-use and run-eval-all alternatives uses an isolated classloader, discarded after the run.

This tool is heavily influenced by Cognitects Transcriptor library, which evaluates repl files in a similar fashion, but here it's evaluation of test-comments withing regular clojure source files, that your favorite dev environment already understand.

# License
Eclipse Public License, same as Clojure. https://www.eclipse.org/legal/epl-v10.html

