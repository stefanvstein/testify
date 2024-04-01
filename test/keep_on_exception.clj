(ns keep-on-exception
  (:require [clojure.test :refer [is deftest]]
            [testify :as t]))

(t/test-comment
  (def a 32)
  (throw (ex-info "Happend" {})))

(deftest on-exception
  (let [n (symbol (str "on-ex-" (. clojure.lang.RT (nextID))))
        thrown? (atom false)]
    (try
      (t/eval-as-use 'keep-on-exception {:unique-ns (fn [_] n)})
      (catch clojure.lang.ExceptionInfo e
        (is (= "Happend" (ex-message e)))
        (try (eval (symbol (name n) "a"))
             (is false (str "An exception sould be thrown since " n " doesn't exist"))
             (catch Exception _
               (reset! thrown? true)))
        (is @thrown?))))
  (let [n (symbol (str "on-ex-" (. clojure.lang.RT (nextID))))]
    (try
      (t/eval-as-use 'keep-on-exception {:unique-ns (fn [_] n)
                                         :keep-ns-on-exception? true})
      (catch clojure.lang.ExceptionInfo e
        (is (= "Happend" (ex-message e)))
        (is (= 32 (eval (symbol (name n) "a"))))))))
