# Repl Test
Running clojure code comments, possibly in a anonymous namespace

You are probably used to evaluate code manually within comment expressions. 
```clojure
(comment 
	(def a 2)
	(+ a 3))
```

You want to elaborate with code by evaluating it step by ste, as you program. It's also likely that you use comment expressions to do some administrative tasks, like poking in a database or similar.

With repl-test you automate evaluation of similar comment expression, named test-comment rather than comment. The test-comment macro is equal to the comment macro, which both simply ignore its body. As soon you have a comment expression which you want to evaluate automaticaly, perhaps as a test, but not your adminstrative routines, you simply rename the comment to test-comment.
```clojure
(test-comment 
	(def a 2)
	(+ a 3))
```

You run content of test-comment expressions with repl-test by supplying the namespace to one of the three functions `run-as-use`, `run-eval-all` or `run-in-ns`. Every step and its result will be printed to *out*   

```clojure
(run-as-use 'project/testcase)
```


`run-as-use` runs content of each test-comment in a new tear-off namespace and refer to the current namespace as refering it as use. All public functions are available. The tear-off namespace is deleted after each test-comment

`run-eval-all` evaluates all forms in namespace in anothe tear-off namespace. All privates are available, except that they belong to the temporary tear-off namespace. The tear-off is removed after each test-comment. The whole namespace is evaluated before test-comments are evaluated. The test-comment is simply a comment during initial evaluation.

`run-in-ns` runs in the already existing name space, not in a tearoff. The namespace remains, possibly altered, afterwards. This is pretty much the same as evaluating step by step manually. 

Since these tests are automated, any exception thrown will stop the process. All remaining test-comments will be ignored on a thrown exception.

A test-case can easily use clojure.test/is to verify facts along the way, and the above run functions can be called withing a deftest.

```clojure
(test-comment 
	(def a 2)
	(+ a 3))
	(is (= 5 *1)))
```


# License
Eclipse Public License, same as Clojure. https://www.eclipse.org/legal/epl-v10.html

