(ns tryout
  (:require [clojure.string :as str] 
            [repl-test :as a]))

*ns*

(defn fun []
  (println "in fun"))


(fun)

#_(a/test-comment (println (str/reverse "tset ni olleH"))
                (throw (ex-info "it happens" {}))
                (fun))


(a/test-comment
 (println "Another test-comment")
 (try (throw (Exception. "exceptional"))
      (catch Exception e (println "Got" e)))
 (fun))


