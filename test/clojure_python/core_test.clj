(ns clojure-python.core-test
  (:use [clojure-python.core] :reload-all)
  (:use [clojure.test]))


(comment
(deftest test-python
  (let [py (python 'py-interp
             :pre-properties {:foo "bar"}
             :post-properties {:bar "foo"}
             :argv ["one" "two" "three"]

             :sys-path []
             :modules [['mod1 {:funcs ['f1 'f2] :objs ['o1 'o2]}]]
             )]))
                           
  )

(deftest test-python-init
  (let [py (python 'py-interp
             :post-properties {"python.home" "/usr/local/my-python"}
             :argv ["one" "two" "three"])]
    (.exec py "import sys")
    (is (= ["one", "two", "three"] (map str (.eval py "sys.argv"))))
    (is (some 
          #(= % "/usr/local/my-python/Lib")
          (map str (.eval py "sys.path"))))))
