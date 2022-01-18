(ns clojurecrazy.transducers
  (:require [criterium.core :refer :all]))

;;; reimplementation of `into`
(defn cc-into [to from]
  (reduce conj to from))

;;; reimplementation of `filterv`
(defn cc-filterv [pred coll]
  (reduce (fn [state input]
            (if (pred input)
              (conj state input)
              state))
          []
          coll))

;;; reimplementation of `mapv`
(defn cc-mapv [f coll]
  (reduce (fn [state input]
            (conj state (f input)))
          []
          coll))

;;; filter + into
(defn cc-filter-into [pred rf init coll]
  (reduce (fn [state input]
            (if (pred input)
              (rf state input)
              state))
          init
          coll))

;;; transducer constructor for `filter`
(defn cc-filter-xf [pred]    ; this is the transducer constructor
  (fn [rf]                   ; this is the transducer
    (fn [state input]        ; this is the new reducing function wrapper
      (if (pred input)       ; here's our filtering logic
        (rf state input)
        state))))

;;; transducer constructor for `map`
(defn cc-map-xf [f]            ; this is the transducer constructor
  (fn [rf]                     ; this is the transducer
    (fn [state input]          ; this is the new reducing function
      (rf state (f input)))))  ; here's where we apply f to the input

;;; reimplementation of `transduce` (arity 4)
(defn cc-xd [xf rf init coll]
  (reduce (xf rf) init coll))

;;; reimplementation of `transduce` (arity 3 and 4)
(defn cc-xd-2
  ([xf rf coll]
   (cc-xd-2 xf rf (rf) coll))
  ([xf rf init coll]
   (reduce (xf rf) init coll)))

;;; reimplementation of `transduce` (arity 3 and 4)
;;; call reduction function arity-1 at completion
(defn cc-xd-3
  ([xf rf coll]
   (cc-xd-3 xf rf (rf) coll))
  ([xf rf init coll]
   (let [rf' (xf rf)]
     (rf' (reduce rf' init coll)))))

(defn cc-partition-all-1 [n]
  (fn [rf]
    (let [p (volatile! [])]             ; reducing function state
      (fn [state input]
        (let [p' (vswap! p conj input)]
          (if (= n (count p'))
            (do (vreset! p [])
                (rf state p'))
            state))))))

(defn cc-partition-all-2 [n]
  (fn [rf]
    (let [p (volatile! [])]             ; reducing function state
      (fn
        ([state]                        ; arity-1
         (if (> (count @p) 0)
           (rf (rf state @p))
           (rf state)))
        ([state input]                  ; arity-2
         (let [p' (vswap! p conj input)]
           (if (= n (count p'))
             (do (vreset! p [])
                 (rf state p'))
             state)))))))

(defn cc-partition-all-3 [n]
  (fn [rf]
    (let [p (volatile! [])]
      (fn
        ([]                             ; arity-0
         (rf))
        ([state]                        ; arity-1
         (if (> (count @p) 0)
           (rf (rf state @p))
           (rf state)))
        ([state input]                  ; arity-2
         (let [p' (vswap! p conj input)]
           (if (= n (count p'))
             (do (vreset! p [])
                 (rf state p'))
             state)))))))


(defn cc-take-while-1 [pred]
  (fn [rf]
    (let [stopped (volatile! false)]
      (fn
        ([]
         (rf))
        ([state]
         (rf state))
        ([state input]
         (cond
           @stopped state
           (pred input) (rf state input)
           :else (do (vreset! stopped true)
                     state)))))))

(defn cc-take-while-2 [pred]
  (fn [rf]
    (fn
      ([]
       (rf))
      ([state]
       (rf state))
      ([state input]
       (if (pred input)
         (rf state input)
         (reduced state))))))
