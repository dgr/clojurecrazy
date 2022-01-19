---
layout: post
title: "Transducers: Middleware for Reducing Functions (Part 4)"
date: 2022-01-19 8:22:00 -0600
---
This is a four-part series. You can find the parts here:
* [Part 1](/clojurecrazy/2022/01/16/transducers-middleware-for-reducing-functions-part-1.html)
* [Part 2](/clojurecrazy/2022/01/17/transducers-middleware-for-reducing-functions-part-2.html)
* [Part 3](/clojurecrazy/2022/01/18/transducers-middleware-for-reducing-functions-part-3.html)
* [Part 4](/clojurecrazy/2022/01/19/transducers-middleware-for-reducing-functions-part-4.html)

Wow! We did it! In our [last episode](/clojurecrazy/2022/01/18/transducers-middleware-for-reducing-functions-part-3.html), we built ourselves a real
transducer that actually works with Clojure's `transduce` function. It
included all three reducing function arities and implemented a
stateful reducing function. It was our own version of `partition-all`.

```clojure
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
```

We're nearing the end of our journey to understand transducers, but
there's a bit more we need to cover.

In `cc-partion-all-3`, we saw that a middleware reducing function can
call the downstream reducing function or not as it pleases, and it can
choose which of the different arities it wants. We saw that the
reduction context (e.g., `transduce`) can signal that the reduction is
done by calling the arity-1 reducing function.

But what if the reduction function decides that the reduction is done
before the reduction context does? How can a reducing function
signal to the reduction context that it wants to stop?

Let's consider a reimplementation of `take-while`.

```clojure
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
```

We start out with `stopped` being `false` and as long as `pred`
applied to the `input` returns true, then we just keep calling the
downstream reducing function. As soon as `pred` returns false, then we
set `stopped` to `true` and return `state` without calling the
downstream reducing function. Once `stopped` is true, we ignore all
further calls and simply return `state` unchanged.

This works great.

```clojure
user> (transduce (cc-take-while-1 #(< % 5)) conj (range 10))
[0 1 2 3 4]
```

"Okay, was that it? Are we done?" you ask.

Not quite. While `cc-take-while-1` works, it's inefficient. Once we
flip `stopped` to `true`, we correctly ignore all further inputs, but
we're still having to process every item in the collection. When there
are only a handful of items, that's no big deal, but it adds up with
large collections.

For instance, let's run some timing tests using
[Criterium](https://github.com/hugoduncan/criterium).

```clojure
user> (let [coll (vec (range 10))]
        (bench (transduce (cc-take-while-1 #(< % 5)) conj coll)))
Evaluation count : 166427760 in 60 samples of 2773796 calls.
             Execution time mean : 359.708462 ns
    Execution time std-deviation : 0.399059 ns
   Execution time lower quantile : 358.765407 ns ( 2.5%)
   Execution time upper quantile : 360.449649 ns (97.5%)
                   Overhead used : 5.939916 ns

Found 4 outliers in 60 samples (6.6667 %)
	low-severe	 2 (3.3333 %)
	low-mild	 1 (1.6667 %)
	high-mild	 1 (1.6667 %)
 Variance from outliers : 1.6389 % Variance is slightly inflated by outliers
nil
user> (let [coll (vec (range 10000))]
        (bench (transduce (cc-take-while-1 #(< % 5)) conj coll)))
Evaluation count : 706140 in 60 samples of 11769 calls.
             Execution time mean : 85.261613 µs
    Execution time std-deviation : 131.542776 ns
   Execution time lower quantile : 84.971377 µs ( 2.5%)
   Execution time upper quantile : 85.464019 µs (97.5%)
                   Overhead used : 5.939916 ns

Found 2 outliers in 60 samples (3.3333 %)
	low-severe	 2 (3.3333 %)
 Variance from outliers : 1.6389 % Variance is slightly inflated by outliers
nil
```

Here, we see that processing ten elements in the collection only takes
us 359.7 nanoseconds, but processing 10,000 elements in the
collection balloons up to 85.2 _microseconds_. So, even though the
result of each call is the same, returning the first five elements,
the second case with 10,000 elements takes much longer.

It doesn't have to be this way, however. Once we set `stopped` to
`true`, we're going to throw away everything else that comes to us. It
would be nice if there was a way to do that. Fortunately, there is.

Clojure has a function named `reduced` that will encapsulate the
return state in a special wrapper that signals to the reduction
context that it should stop processing. The reduction context can use
a predicate named `reduced?`  to check the newly returned state value
to see if it's wrapped in this special wrapper. You can pull the final
state value out of the wrapper using `@` (`deref`).

```clojure
user> (reduced [1 2 3])
#<Reduced@56e48adb: [1 2 3]>
user> (reduced? (reduced [1 2 3]))
true
user> (reduced? [1 2 3])
false
user> @(reduced [1 2 3])
[1 2 3]
```

So, we can stop the reduction in our version of `take-while` by simply
returning a `reduced` value as soon as the predicate returns `false`.

```clojure
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
```

Note that this also allows us to remove the state in the reducing
function. When we're done, we're done.

You can see the impact of this on our ten vs. 10,000 elements test.

```clojure
user> (let [coll (vec (range 10))]
        (bench (transduce (cc-take-while-2 #(< % 5)) conj coll)))
Evaluation count : 209638200 in 60 samples of 3493970 calls.
             Execution time mean : 284.600497 ns
    Execution time std-deviation : 1.499647 ns
   Execution time lower quantile : 282.564290 ns ( 2.5%)
   Execution time upper quantile : 287.687909 ns (97.5%)
                   Overhead used : 5.939916 ns
nil
user> (let [coll (vec (range 10000))]
        (bench (transduce (cc-take-while-2 #(< % 5)) conj coll)))
Evaluation count : 211362120 in 60 samples of 3522702 calls.
             Execution time mean : 280.628692 ns
    Execution time std-deviation : 0.486964 ns
   Execution time lower quantile : 279.166659 ns ( 2.5%)
   Execution time upper quantile : 281.293161 ns (97.5%)
                   Overhead used : 5.939916 ns

Found 2 outliers in 60 samples (3.3333 %)
	low-severe	 2 (3.3333 %)
 Variance from outliers : 1.6389 % Variance is slightly inflated by outliers
nil
```

Now, the timings are both about the same, 280 nanoseconds or so. They
both process the first five elements of the collection and then return
`(reduced state`, immediately stopping the reduction.

With that, our multi-part series diving into transducers comes to a
close. Hopefully, this has demystified the subject and you understand
now why I say that transducers are just middleware for reducing
functions. In this series, you've learned how to implement your own
transducers, including transducers that return stateful reducing
functions. You've seen how to clean up state at the end of a reduction
using the arity-1 reducing function and how to stop the reduction
process using `reduced` as soon as the reducing function determines
that it's pointless to continue. You should now have the skills to
make use of standard transducers and write your own when your programs
require it.

You can find all the source code from this series in the
[Clojure Crazy GitHub repository](https://github.com/dgr/clojurecrazy/blob/4d8f4a1b25aac7dcfd7a90e0483fe5d405552a50/src/clojurecrazy/transducers.clj).
