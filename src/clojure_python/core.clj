(ns clojure-python.core
  (:use (clojure.core.*))
  (:import (org.python.util PythonInterpreter)
           (org.python.core.*)))

; instantiate a python interpreter in the python namespace
(def *interp* (new org.python.util.PythonInterpreter))

(defn init 
  "this may later take keywords and initialize other things
  for now it is just used to specify python library paths"
  ([libpath]
     (doto clojure-python.core/*interp* 
       (.exec "import sys")
       (.exec (str "sys.path.append('" libpath "')"))))
  ([libpath & more]
     (init libpath)
     (apply init more))) 

(defmacro py-import
  "define a library using the same name it has in python
  if multiple arguments are given, the first is assumed 
  to be a library that has been imported, 
  and the others are objects to import from this library"
  ([] nil)
  ([lib] ; import a library
     `(do (. clojure-python.core/*interp* exec (str "import " ~(name lib)))
          (def ~lib (. (. (. clojure-python.core/*interp* getLocals) 
                              __getitem__ ~(name lib)) 
                           __dict__))
          (print ~lib)))
  ([lib object] ; import object from a library
     `(do (def ~object  (.__finditem__ 
                         ~lib 
                         ~(name object))) 
          (print ~object)))
  ([lib object & more-objects] ; import multiple objects
     `(do (py-import ~lib ~object) 
          (py-import ~lib ~@more-objects))))

(defmacro import-fn 
  "this is like import but it defines the imported item 
  as a native function that applies the python wrapper calls"
  ([lib fun] 
     `(def ~fun (py-fn ~lib ~fun)))
  ([lib fun & more-funs]
     `(do (import-fn ~lib ~fun)
          (import-fn ~lib ~@more-funs))))

(defmacro py-fn [lib fun]
  "create a native clojure function applying the python 
  wrapper calls on a python function at the top level of the library
  use this where lambda is preferred over named function"
  `(let [f# (.__finditem__ 
             ~lib 
             ~(name fun))]
     (fn [& args#]
       (call f# args#))))

(defmacro __ 
  "access attribute of class or attribute of attribute of (and so on) class"
  ([class attr]
     `(.__findattr__ ~class ~(name attr)))
  ([class attr & more]
     `(__ (__ ~class ~attr) ~@more)))

(defmacro _> 
  "call attribute as a method
  basic usage: (_> [class attrs ...] args ...)
  usage with keyword args: (_> [class attrs ...] args ... :key arg :key arg)
  keyword args must come after any non-keyword args"
  ([[class & attrs] & args]
     (let [keywords (map name (filter keyword? args))
           non-keywords (filter (fn [a] (not (keyword? a))) args)]
       `(call (__ ~class ~@attrs) [~@non-keywords] ~@keywords))))

(defn dir [x]
  "it's slightly nicer to call the dir method in this way"
  (seq (.__dir__ x)))

(defn iter [o start end]
  "Jython sometimes represents things as 'PyObjectDerived', which has no direct method to get their items as a seq"
  (for [i (range start end)]
    (.__getitem__ o i)))

(defn java2py [args]
  "to wrap java objects for input as jython, and unwrap jython output as java
  (thanks to Marc Downie on Clojure list for suggesting this)"
  (into-array 
   org.python.core.PyObject 
   (map 
    (fn [n] (. org.python.core.Py java2py n)) 
    args)))

(defn call [fun args & key-args]
  "The first len(args)-len(keywords) members of args[] 
  are plain arguments. The last len(keywords) arguments
  are the values of the keyword arguments."
  (.__tojava__ 
   (if key-args
     (.__call__ fun (java2py args) (into-array java.lang.String key-args))
     (.__call__ fun (java2py args)))
    Object))