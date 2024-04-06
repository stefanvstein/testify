# Testify - Evaluate Clojure comments automatically

<img align="right" src="robby.jpg" width="150" height="150">

Your `comment` expression became a usable snippet of code! 

**Testify** turns selected comments into automatically evaluated scripts, while remaining embedded within your code. Rename the `comment` to `test-comment` and evaluate with `(eval-in-ns 'your-namespace)`.

Perhaps you want to evaluate comments as example based tests. Use **Testify** to evaluate your test-comment from within your traditional test, saving you the hassle of converting your comment, or REPL structured code, into a test function. The `test-comment` is just another empty macro, ignoring its body, that `eval-in-ns` and its siblings recognize. 

While `eval-in-ns` evaluate each expression as is, in place, its sibling `eval-as-use` evaluate expressions from within a foreign namespace, which probably is more suitable for possibly polluting tests.

[![Clojars Project](https://img.shields.io/clojars/v/org.clojars.vstein/testify.svg)](https://clojars.org/org.clojars.vstein/testify)
## Background

You are probably used to evaluate code manually within comment expressions, Rich comments.

*This is very similar to writing in a repl, but you select and evaluate an expression in the source code file rather than writing the expression up front. These expressions are hidden behind comments since they are expressions to interact with, rather than being part of the system. This is a very popular development technique in lisps, like Clojure.*

You have comments like this in you source files:

```clojure
(comment
  (def a 2)
  (+ a 3))
```
Var `a` becomes defined in the namespace when you evaluate these statements manually, rather than when you run the system, as it is embedded in a comment. Traditional example based tests, using e.g. clojure.test, are normally defined as functions, rather than as the result of interactive fiddling.

It's also likely that you use comment expressions to do some administrative tasks, like poking in a database or similar, that you probably don't want automated, but keep as interactive snippets, close to related code.

## How?

**Testify** automate evaluation of similar comment expressions, named `test-comment` rather than `comment`. The test-comment macro is equal to the traditional `comment` macro, as both simply ignore its body. As soon you have a comment expression which you want to evaluate automatically, perhaps as a test, you simply rename the `comment` to `test-comment`.

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
You evaluate content of top level test-comment expressions by supplying the namespace to one of the three functions `eval-as-use`, `eval-all` or `eval-in-ns`, that recognize the test-comment.

*Note that only the content of top level test-comments in the source file will be evaluated. Nested test-comments will just be like ordinary comments, ignored.*

Other parts of the the source file will be considered before evaluating the test-comments, slightly different depending on evaluation method. The test-comments are thereafter evaluated from top to bottom as they appear.

*Note that it is the source code that is being evaluated, hence the source code has to be available for the classloader. Only well known source code should be evaluated as the clojure reader is not designed for reading unknown data.*

Every step and its result will be printed to \*out\*, like:

```
(clojure.core/in-ns 'project.testcase)
=> #namespace[project.testcase]

(def a 2)
=> #'project.testcase/a

(+ a 3)
=> 5
```
## Alternatives
There are different evaluation functions, `eval-in-ns`, `eval-as-use` and `eval-all`, having slightly different behavior:

`eval-in-ns` evaluates the content of the test-comments in its already existing namespace. This is pretty much the same as evaluating step by step manually as usually done with comment. But this is not always great for automation as the namespace remains, possibly altered.

`eval-as-use` evaluates content of each test-comment in a new tear-off namespace and refer to the current namespace as refered by use, as depicted blow. All public functions are available. The tear-off namespace is deleted after each test-comment

`eval-all` evaluates all forms in namespace in another tear-off namespace. All vars are available, including private, except that they belong to the new temporary tear-off namespace. The whole tear-off namespace is evaluated before and removed after, each test-comment is evaluated. The test-comment is ignored, as a comment, during initial evaluation.

*Var `a`, in examples above, is not present in namespace project.testcase, when using `eval-as-use` or `eval-all` as it is placed in the tear-off namespace*

Since these tests are automated, any exception thrown will stop the process. All remaining test-comments will be ignored when an exception is thrown.

A test-case can easily use clojure.test/is to verify facts along the way. The above eval- functions can be evaluated withing a deftest.

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
Automated evaluation and their result is printed to \*out\*, including preparations like evaluating namespace. It should be easy to understand automated evaluation by reading \*out\*. Note tear-off namespace `project.testcase-9379`:

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

The `eval-as-use` and `eval-all` alternatives uses an isolated classloader, discarded after the evaluation run. Testify expects source files with a leading ns form, which translates to a in-ns, followed by individual requirements. The resulting namespace is not considered a loaded lib by Clojure.

## Other comments

It's possible to use other, home grown, comments with testify:

```clojure
(ns project.tests
  (:require [testify :refer [eval-as-usen]]
            [clojure.test :refer [deftest is]]))

;; test-comment is, just like comment, a macro that
;; ignores its body. It's easy to create your own.

(defmacro unit-test [& _])

;; and perhaps have a more meaningful name, like unit-test:

(unit-test
  (def a 2)
  (+ a 3)
  (is (= 5 *1)))

;; Supply an optional parameter to pick the "comment macro" 
;; you want to evaluate:

(deftest test-the-test
  (eval-as-use 'project.tests  #'unit-test))
```

Additional vars can categorize comments further.

## Options

The eval- functions takes an optional either var, as seen above, or map of options. The var is shortcut for map contining the key :test-var with the option as value. Following options are available:

`:test-var` is a var of the macro used as test-comment. Default value is `#'testify/test-comment`. It can be changed to make selective evaluation runs in the same source file.

*Note that `eval-all` will evaluate the whole file representing the namespace into a new anonymous namespace. If the optional test-comment macro is defined here, it will most likely not match when defined in test-var option. Just put additional test-comment definitions in another namespace when using `eval-all`.*

`:new-classpath?` is a boolean for whether each test-comment should be evaluated in a new class-loader, so that locally defined types are omitted after each test comment. This is set for `eval-as-use` and `eval-all`.

`:keep-ns-on-exception?` is set to prevent removing any tear-off name space on thrown exception. Can be used to investigate circumstances.

`:use-target?` tells whether the target namespace should be referred as `use`. Set by default in `eval-as-use`.

`:unique-ns` an optional function translating a namespace symbol to a symbol used for tear-off namespace. Can be used to override default, as a predictive alternative.

## Influence

Testify is heavily influenced by Cognitects Transcriptor library, which evaluates repl files in a similar fashion. Repl files are though not regular clojure source files, that your favorite dev environment already understands.

## Alternative tools

Transcriptor https://github.com/cognitect-labs/transcriptor is surely still an alternative tool, even though it evaluate repl files rather than comments.

RCF https://github.com/hyperfiddle/rcf is another tool that automate evaluation in the REPL. It is oriented around sending the whole comment expression, called tests, to the REPL. Under the hood it is driven by a more advanced macro, that switches in behavior depending on state. It expands to either wrapping content into a do or nothing at all. **Testify** will never change behaviour of macros. RCF can also translate content of its macros to clojure.test tests.

## Contact

Questions or ideas? Please ping me at Clojurians: @stefanvstein

# License

Eclipse Public License, same as Clojure. https://www.eclipse.org/legal/epl-v10.html
