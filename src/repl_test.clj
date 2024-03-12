(ns repl-test
  (:import [clojure.lang LineNumberingPushbackReader])
  (:require
   [clojure.core.server :as server]
   [clojure.java.io :as io]
   [clojure.main :as main]
   [clojure.pprint :as pp]
   [clojure.string :as str]))



(defmacro test-comment
  "A test to be run in a repl session. repl-test will run contained
  statement automitically, in a another namespace with everything in
  this namespace referred.
  It's like a comment, ignores body, yields nil"
  [& _])

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
                    [(list 'clojure.core/use (list 'quote the-ns))])]
      {:forms (concat ns-def
                      clojure-ns
                      refers
                      use-def)})))

(defn only-ns [form]
  (when (ns? form)
    (let [[_ the-ns] form]
      {:forms [(list 'clojure.core/in-ns
                     (list 'quote the-ns))]})))

(defn test-form [form {:keys [active-comment] :as config}]

  (when (and
         (list? form)
         (symbol? (first form))
         (= active-comment (resolve (first form))))
    (if (:has-run-form config)
      {:config (assoc config :there-is-more-form true)
       :forms []}
      (let [conf (-> config
                     (update :run-at-form #(if-not % 0 %))
                     (update :current-form #(if % (inc %) 0)))]
        (if (= (:current-form conf) (:run-at-form conf))
          {:config (-> conf

                       (update :run-at-form inc)
                       (dissoc :current-form)
                       (assoc :has-run-form true))
           :forms (next form)}
          {:config conf :forms []})))))

(defn any-form [form]
  {:forms [form]})


(defn require-and-use [input config]
    (let [ns-input (translate-ns input config)
          test-input (test-form input config)]
      (or
       ns-input
       test-input
       {:forms []})))

(defn eval-all [input config]
  (let [ns-input (translate-ns input config)
        test-input (test-form input config)
        any-input (any-form input)]
    (or
     ns-input
     test-input
     any-input)))

(defn only-test [input config]
  (or (only-ns input)
      (test-form input config)
      {:forms []}))

(def ^:private request-prompt (Object.))
(def ^:private request-exit (Object.))

(defmacro with-another-classloader [& body]
  `(let [cl# (.getContextClassLoader (Thread/currentThread))]
     (try (.setContextClassLoader (Thread/currentThread)
                                  (clojure.lang.DynamicClassLoader. cl#))
          (do ~@body)
          (finally (.setContextClassLoader (Thread/currentThread) cl#)))))

(defn read-and-eval [config]
  (let [read-eval *read-eval*]
    ((fn fun [config]
         (let [input (main/with-read-known
                       (server/repl-read request-prompt
                                         request-exit))]
           (cond (= request-prompt input) (recur config)
                 (= request-exit input) config
                 :else (let [{config :config inputs :forms :or {config config} :as all} ((:input-selector config) input config)]
                         (when inputs
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
                               (when inputs (recur (first inputs) (next inputs)))))
                           (recur config))))))
     config)))

(defn repl [{:keys [new-classpath?] :as config}]

  (main/with-bindings
    (if new-classpath?
      (with-another-classloader
        (read-and-eval config))
      (read-and-eval config))))

(defn as-path [s]
  (str (.. s
           (replace \- \_)
           (replace \. \/))))

(defn- repl-on
  [ns config]
  (let [ns-str (name ns)
        path (as-path ns-str)
        cl (clojure.lang.RT/baseLoader)]
   (let [conf  (binding [*ns* *ns*]
                 (with-open [rdr (-> (or (.getResourceAsStream cl (str path ".clj"))
                                         (.getResourceAsStream cl (str path ".cljc"))
                                         (throw (ex-info (str "Can't source for " ns-str)
                                                         {:looking-for (str path ".clj")})))
                                     io/reader
                                     LineNumberingPushbackReader.)]
                   (binding [*source-path* (str ns-str) *in* rdr]
                     (repl config))))]
     (if (:there-is-more-form conf)
       (recur ns (dissoc conf :there-is-more-form :has-run-form))
       conf))))


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
