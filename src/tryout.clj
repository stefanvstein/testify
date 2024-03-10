(ns tryout
  (:require [clojure.string :as str] 
            [repl-test :as a]))

*ns*

(defn fun []
  (println "in fun"))


(fun)

(a/test-comment
 (println (str/reverse "tset ni olleH"))
 #_(throw (ex-info "it happens" {}))
 (fun))


(a/test-comment
 (println "Another test-comment")
 (fun))

