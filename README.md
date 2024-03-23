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
  (:require [repl-test :refer [test-comment run-as-use]]))
  
(test-comment 
  (def a 2)
  (+ a 3))
  
(comment 
  ;;space for more experimental and administrative tasks
  (run-as-use 'project.testcase))
  

```
You run content of test-comment expressions with repl-test by supplying the namespace to one of the three functions `run-as-use`, `run-eval-all` or `run-in-ns`. Every step and its result will be printed to \*out\*, here in a tear-off namespace, indicated by uniqueness added to the namespace name. 
```
(clojure.core/in-ns 'project.testcase-9244)
=> #namespace[project.testcase-9244]

(clojure.core/use 'clojure.core)
=> nil

(clojure.core/require
 '[repl-test :refer [test-comment run-as-use]])
=> nil

(clojure.core/use 'project.testcase)
=> nil

(def a 2)
=> #'project.testcase-9244/a

(+ a 3)
=> 5

(clojure.core/remove-ns 'project.testcase-9244)
=> #namespace[project.testcase-9244]
```

`run-as-use` runs content of each test-comment in a new tear-off namespace and refer to the current namespace as referring it as use, as depicted above. All public functions are available. The tear-off namespace is deleted after each test-comment

`run-eval-all` evaluates all forms in namespace in another tear-off namespace. All privates are available, except that they belong to the temporary tear-off namespace. The tear-off is removed after each test-comment. The whole namespace is evaluated before test-comments are evaluated. The test-comment is simply a comment during initial evaluation.

`run-in-ns` runs in the already existing name space, not in a tear-off. The namespace remains, possibly altered, afterwards. This is pretty much the same as evaluating step by step manually as we usually do, but not always that great for automated testing. There's classloader isolation and no cleaning up afterwards. 

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
Automated evaluation and their result is printed to \*out\*, including preparation like evaluating namespace. It should be easy to understand automated evaluation.

The `run-as-use` and `run-eval-all` alternatives uses an isolated classloader, discarded after the run. The tools expects a leading ns form, but translates it to a in-ns, followed by individual requirements. The resulting namespace is not considered a loaded lib, by Clojure.

This tool is heavily influenced by Cognitects Transcriptor library, which evaluates repl files in a similar fashion, which is not regular clojure source files, that your favorite dev environment already understand.

# Options

The run- functions takes a optional map of options: 

`:test-comment` is a var of the macro used as test comment default value `#'repl-test/test-comment`. It can be changed to make selective runs in the same namespace. 

*Note that `run-eval-all` will evaluate the whole file representing the name space into a new anonymous namespace. If the optional test comment macro is defined here, it will most likely not match that when defined in test-comment option. Just put additional test-comment definitions in another namespace when using `run-eval-all`.* 

`:new-classpath?` is a boolean telling whether each test-comment should run in a new class-loader, so that locally defined types are omitted after each test comment. This is set for `run-as-use` and `run-eval-all`.

`:keep-ns-on-exception?` is a boolean that when set omits removing any tear-off name space on thrown exception. Can be used to investigate circumstances

The other options `:execute-case`, `:current-case`, `:case-executed?`, `:forms`, `:there-is-more-form`, `:context` and `:remove-ns` are used internally and is subject to change

# License
Eclipse Public License, same as Clojure. https://www.eclipse.org/legal/epl-v10.html

