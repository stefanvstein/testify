(ns repl-test
  (:import [clojure.lang LineNumberingPushbackReader])
  (:require
   [clojure.core.server :as server]
   [clojure.java.io :as io]
   [clojure.main :as main]
   [clojure.pprint :as pp]
   [clojure.string :as str]))

(defn write [print-writers txt]
  (doseq [w print-writers]
          (.println w txt)
          (.flush w)))

(defn close [print-writers]
  (doseq [w print-writers]
          (.close w)))




(defn- filter-key [keyfn pred amap]
  (loop [ret {} es (seq amap)]
    (if es
      (if (pred (keyfn (first es)))
        (recur (assoc ret (key (first es)) (val (first es))) (next es))
        (recur ret (next es)))
      ret)))

(defn ns-privates
  [ns]
  (let [ns (the-ns ns)]
    (filter-key val (fn [^clojure.lang.Var v] 
                      (and (instance? clojure.lang.Var v)
                           (= ns (.ns v))
                           (not (.isPublic v))))
                (ns-map ns))))

 (defn refer-privates
  [ns-sym]
  (let [ns (or (find-ns ns-sym)
               (throw (new Exception (str "No namespace: " ns-sym))))
        nsprivate (ns-privates ns)
        to-do (keys nsprivate)]

    (for [sym to-do]

      (let [v (nsprivate sym)]
        (when-not v
          (throw (new java.lang.IllegalAccessError
                      (str sym " does not exist"))))
        (list 'def sym v))))) 

(defmacro test-comment
  "A test to be run in a repl session. repl-test will run contained 
  statement automitically, in a another namespace with everything in 
  this namespace referred. 
  It's like a comment, ignores body, yields nil"
  [& _])

(def ^:private ^:dynamic *exit-items* ::disabled)

(defn unique-ns [prefix]
  (let [un (symbol (str prefix "-" (. clojure.lang.RT (nextID))))]
    (if-not (find-ns un)
      un (recur prefix))))

(defn- make-fun-call [[kv & vs]]
  (let [fun (symbol (str "clojure.core/"
                         (name kv)))]
    (cond-> [fun]
      vs (conj vs))))

(defn- quote-vals [[fun values]]
  (cons fun (->> values
                 (map (partial list 'quote))
                 not-empty)))

(defn- has-clojure-core? [[f vs]]
  (or (= f 'clojure.core/refer-clojure)
      (some (fn [v]
              (or (= 'clojure.core v)
                  (and (sequential? v)
                       (= 'clojure.core (first v)))))
            vs)))
(defn- ns? [form]
  (and (list? form)
       (symbol? (first form))
       (= 'ns (first form))
       (symbol? (second form))))

(defn translate-ns
  "Translates ns instructions like the ns macro, but renames the ns by adding a postfix name. Does not affect *loaded-libs* , doc or any other namespace meta data, nor touch thread binding. nil on any other form"
  [form {:keys [use-target?]}]
  (when (ns? form)
    (let [[_ the-ns & args] form
          params-without-doc (if (string? (first args))
                               (next args)
                               args)
          fun-values  (map make-fun-call params-without-doc)

          refers (map quote-vals fun-values)
          clojure-ns (when-not (some has-clojure-core? fun-values)
                       [(list 'clojure.core/use ''clojure.core)])
          ns-def [(list 'clojure.core/in-ns
                        (list 'quote (unique-ns the-ns)))]
          use-def (when use-target? 
                    [(list 'clojure.core/use (list 'quote the-ns))])
;;; What should we do with private vars? Especially private values?. Should we move them to current ns, or refer them in place. Why do we have a namespace? Should we load everything in current namespace?
          #_#_privates (refer-privates the-ns)]
      (concat ns-def
              clojure-ns
              refers
              use-def

              #_privates))))

(defn only-in-ns [form]
  (when (ns? form)
    (let [[_ the-ns] form]
      [(list 'clojure.core/in-ns
             (list 'quote the-ns))])))

(defn test-form [form {:keys [active-comment]}]
  (when (and (list? form)
             (symbol? (first form))
             (= active-comment (resolve (first form))))
    (next form)))

(defn any-form [form]
  [form])

(defn require-and-use [input config]
  (let [ns-input (translate-ns input config)
        test-input (test-form input config)
        ]
    (or
     ns-input
     test-input
     [])))

(defn eval-all [input config]
  (let [ns-input (translate-ns input config)
        test-input (test-form input config)
        any-input (any-form input)]
    (or
     ns-input
     test-input
     any-input)))

(defn only-test [input config]
  (or (only-in-ns input)
      (test-form input config) []))

(def ^:private request-prompt (Object.))
(def ^:private request-exit (Object.))

(defn read-eval-print [{:keys [input-selector]
                        :as config}]
  (let [read-eval *read-eval*
        input (main/with-read-known (server/repl-read request-prompt request-exit))]
    (if (#{request-prompt request-exit} input)
      input
      (let [inputs (input-selector input config)]
        (loop [input (first inputs) inputs (next inputs)]
          
          (when input
            (pp/pprint input)
            (let [value (binding [*read-eval* read-eval] (eval input))]
              (set! *3 *2)
              (set! *2 *1)
              (set! *1 value)
              (print "=> ")
              (pp/pprint value)
              (println))
            (when inputs (recur (first inputs) (next inputs)))))))))

(defn repl
  "Transcript-making REPL. Like a normal REPL except:
  
- pretty prints inputs
- prints '=> ' before pretty printing results
- throws on exception

Not intended for interactive use -- point this at a file to
produce a transcript as-if a human had performed the
interactions."
  [{:keys [new-classpath?] :as config}]
  (let [cl (.getContextClassLoader (Thread/currentThread))]
    (try
      (when new-classpath?
        (.setContextClassLoader (Thread/currentThread)
                                (clojure.lang.DynamicClassLoader. cl)))
      (main/with-bindings
        (binding [*exit-items* (atom ())]
          (try
            (loop []
              (let [value (read-eval-print config)]
                (when-not (identical? value request-exit)
                  (recur))))
            (finally
              (doseq [item @*exit-items*]
                (item))))))
      (finally
        (when new-classpath?
          (.setContextClassLoader (Thread/currentThread) cl))))))

(defn as-path [s]
  (str (.. s
           (replace \- \_)
           (replace \. \/))))

(defn- repl-on
  [ns config]
  (let [ns-str (name ns)
        path (as-path ns-str)
        cl (clojure.lang.RT/baseLoader)]
    (binding [*ns* *ns*]
      (with-open [rdr (-> (or (.getResourceAsStream cl (str path ".clj"))
                              (.getResourceAsStream cl (str path ".cljc"))
                              (throw (ex-info (str "Can't source for " ns-str)                                                      {:looking-for (str path ".clj")})))
                          io/reader
                          LineNumberingPushbackReader.)]
        (binding [*source-path* (str ns-str) *in* rdr]
          (repl config))))))

(defn run-as-use
  "Run content in test-comment of ns in a tearoff namespace where ns is refered."
  [ns]
  (repl-on ns {:input-selector require-and-use
               :use-target? true
               :new-classpath? true
               :active-comment (var test-comment)}))

(defn run-eval-all
  "Run content in test-comment of ns in a tearoff namespace in which all of ns is evaluated."
  
  [ns]
  (repl-on ns {:input-selector eval-all
               :new-classpath? true
               :active-comment (var test-comment)}))

(defn run-in-ns
  "Run content in test-comment of ns in it's own namespace"
  [ns]
  (repl-on ns {:input-selector only-test
               :active-comment (var test-comment)}))
(comment
  (run-as-use 'tryout)
  (run-eval-all "tryout")
  (run-in-ns 'tryout)
  
  #_no )



