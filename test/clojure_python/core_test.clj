(ns clojure-python.core-test
  (:use [clojure-python.core] :reload-all)
  (:use [clojure.test]))

(deftest replace-me ;; FIXME: write
  (is false "No tests have been written."))

(deftest test-python
  (let [py (python
             :pre-properties {:foo "bar"}
             :post-properties {:bar "foo"}
             :argv ["one" "two" "three"]
             :sys-path []
             :modules ['mod1 {:function ['f1 'f2] :objs ['o1 'o2]}])]))
                           

