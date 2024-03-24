(ns testify.core
  (:require
   [clojure.core.server :as server]
   [clojure.java.io :as io]
   [clojure.main :as main]
   [clojure.pprint :as pp])
  (:import
   [clojure.lang LineNumberingPushbackReader]))

(defn unique-ns [context prefix]
  (if-let [fun (:unique-ns context)]
    (fun prefix)
    (let [next-id (. clojure.lang.RT (nextID))
          un (symbol (str prefix "-" next-id))]
      (if-not (find-ns un)
        un
        (recur context prefix)))))

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
  [form {:keys [use-target?]
         :as context}]
  (when (ns? form)
    (let [[_ the-ns & args] form
          uns (unique-ns context the-ns)
          params-without-doc (if (string? (first args))
                               (next args)
                               args)
          fun-values  (map make-fun-call params-without-doc)

          refers (map quote-vals fun-values)
          clojure-ns (when-not (some has-clojure-core? fun-values)
                       [(list 'clojure.core/use ''clojure.core)])
          ns-def [(list 'clojure.core/in-ns
                        (list 'quote uns))]
          use-def (when use-target?
                    [(list 'clojure.core/use (list 'quote the-ns))])]

      {:context (assoc context :remove-ns uns)
       :forms (concat ns-def
                      clojure-ns
                      refers
                      use-def)})))

(defn only-ns [form]
  (when (ns? form)
    (let [[_ the-ns] form]
      {:forms [(list 'clojure.core/in-ns
                     (list 'quote the-ns))]})))
(defn test-form? [test-comment form]
  (and
   (list? form)
   (symbol? (first form))
   (= test-comment (resolve (first form)))))

(defn inc-or-zero [x]
  (if x (inc x) 0))

(defn or-zero [x]
  (or x 0))

(defn executed-context [context]
(-> context
    (update :execute-case inc)
    (dissoc :current-case)
    (assoc :case-executed? true)))

(defn test-form [form {:keys [test-comment run-all-cases? case-executed?] 
                       :as context}]
  (when (test-form? test-comment form)
    (let [content (next form)
          current (-> context
                      (update :execute-case or-zero)
                      (update :current-case inc-or-zero))
          should-run? (= (:current-case current)
                         (:execute-case current))]
      (cond run-all-cases? {:forms content}
            case-executed? {:context (assoc context :there-is-more-form true)}
            should-run? {:context (executed-context current)
                         :forms content}
            :else {:context current}))))

(defn any-form [form]
  {:forms [form]})


(defn require-and-use [input context]
    (let [ns-input (translate-ns input context)
          test-input (test-form input context)]
      (or
       ns-input
       test-input)))

(defn eval-all [input context]
  (let [ns-input (translate-ns input context)
        test-input (test-form input context)
        any-input (any-form input)]
    (or
     ns-input
     test-input
     any-input)))

(defn only-test [input context]
  (or (only-ns input)
      (test-form input context)))

(def ^:static request-prompt (Object.))
(def ^:static request-exit (Object.))

(defmacro with-another-classloader [& body]
  `(let [cl# (.getContextClassLoader (Thread/currentThread))]
     (try (.setContextClassLoader (Thread/currentThread)
                                  (clojure.lang.DynamicClassLoader. cl#))
          (do ~@body)
          (finally (.setContextClassLoader (Thread/currentThread) cl#)))))

(defn request-prompt? [input]
  (= request-prompt input))

(defn request-exit? [input]
  (= request-exit input))

(defn eval-print [read-eval input]
  (let [value (binding [*read-eval* read-eval] (eval input))]
    (set! *3 *2)
    (set! *2 *1)
    (set! *1 value)
    (print "=> ")
    (pp/pprint value)
    (println)))

(defn final [read-eval context]
  (when-let [rns (:remove-ns context)]
    (let [form (list 'clojure.core/remove-ns (list 'quote rns))]
      (pp/pprint form)
      (eval-print read-eval form)))
  (dissoc context :remove-ns))

(defn read-and-eval [{:as context
                      :keys [input-selector
                             read-eval]}]
  (let [input (main/with-read-known
                (server/repl-read request-prompt
                                  request-exit))]
    (cond (request-prompt? input) (recur context)
          (request-exit? input) (final read-eval context)
          :else (let [{context :context
                       inputs :forms
                       :or {context context
                            inputs []}}
                      (input-selector input context)]
                  (doseq [input inputs]
                    (pp/pprint input)
                    (try
                      (eval-print read-and-eval input)
                      (catch Exception e
                        (when-not (:keep-ns-on-exception? context)
                          (final read-eval context))
                        (throw e))))

                  (recur context)))))


(defn repl-with [{:keys [new-classpath?] :as context} rdr ns-str]
  (with-open [rdr rdr]
    (binding [*ns* *ns*
              *source-path* ns-str
              *in* rdr]
      (main/with-bindings
        (let [ctx (assoc context :read-eval *read-eval*)]
          (if new-classpath?
            (with-another-classloader
              (read-and-eval ctx))
            (read-and-eval ctx)))))))

(defn as-path [s]
  (str (.. s
           (replace \- \_)
           (replace \. \/))))

(defn source-reader [ns-str]
  (let [path (as-path ns-str)
        cl (clojure.lang.RT/baseLoader)]
    (-> (or (.getResourceAsStream cl (str path ".clj"))
            (.getResourceAsStream cl (str path ".cljc"))
            (throw (ex-info (str "Can't source for " ns-str)
                            {:looking-for (str path ".clj")})))
        io/reader
        LineNumberingPushbackReader.)))

(defn repl
  [ns current-context]
  (let [ns-str (name ns)
        context (repl-with current-context (source-reader ns-str) ns-str)]

    (if (:there-is-more-form context)
      (recur ns (dissoc context :there-is-more-form :case-executed?))
      context)))
