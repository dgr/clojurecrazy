---
layout: post
title: "Transducers: Middleware for Reducing Functions (Part 3)"
date: 2022-01-18 8:02:00 -0600
---
Welcome back. In our [last episode](/clojurecrazy/2022/01/17/transducers-middleware-for-reducing-functions-part-2.html), we got within spitting distance of
"real" transducers. We saw how transducers are middleware wrapped
around a reducing function. And we actually wrote some basic
transducers.

In this episode, we'll pull together some of the last bits and have
you writing _real_ transducers that will work with Clojure's standard
transduction functions (e.g., `transduce`). We'll cover the multiple
arities that reducing functions need to have. We'll even write a
"stateful transducer" and learn why that's an inaccurate name. Strap
in; we have a lot to get to and this is going to move fast.

If you were paying attention during the last episode, you might have
noticed that our `cc-xd` function looks _a lot_ like Clojure's
standard `transduce` function.

```clojure
user> (defn cc-xd [xf rf init coll]
        (reduce (xf rf) init coll))
#'user/cc-xd
user> (doc transduce)
-------------------------
clojure.core/transduce
([xform f coll] [xform f init coll])
  reduce with a transformation of f (xf). If init is not
  supplied, (f) will be called to produce it. f should be a reducing
  step function that accepts both 1 and 2 arguments, if it accepts
  only 2 you can add the arity-1 with 'completing'. Returns the result
  of applying (the transformed) xf to init and the first item in coll,
  then applying xf to that result and the 2nd item, etc. If coll
  contains no items, returns init and f is not called. Note that
  certain transforms may inject or skip items.
nil
```

Both functions take the same arguments in the same order. There is
some difference in terminology, however. First, I use the term `xf`
where the doc-string uses `xform`. The doc-string names the result of
applying `xform` to `rf` as `xf`. Personally, I think the doc-string
is confusing and different official Clojure documentation about
transducers and different blogs use different argument names in
different spots. I think this overall inconsistency, coupled with the
functions-returning-anonymous-functions-that-return-anonymous-functions
nature of the problem, is part of the reason that folks have been so
confused about transducers.

So, I've tried to be consistent throughout this blog series:
* A reducing function is always named `rf`.
* A transducer is always named `xf`.
* Since transducers compose with `comp`, a stack of transducers is
  also a transducer and is named `xf`.
* The result of applying a transducer to a reducing function is
  another reducing function, so the result is named `rf` (if it's
  actually given a name at all).
* The names of the arguments to a reducing function are names `state`
  and `input`.
* When we compose individual transducers together, I have called that
  a "transducer stack" to distinguish it from a single, atomic
  transducer.
* The "reduction context" is the logic that is calling the reducing
  function to perform the processing. Example contexts are `transduce`,
  `reduce`, or even `core.async` channels.

Now, back to `cc-xd` and `transduce`. The main difference between the
two is that our version always takes four arguments while the standard
version of `transduce` has a three argument version. The doc-string
says that if the initial value of the collection is not supplied, then
the reducing function of zero arity is called to produce it. In other
words, it invokes `(rf)` (without any transducers wrapped around it)
to create the initial value. You can see that a function like `conj`
produces an initial value if you call it with no arguments. So does
`+` or `*`.

```clojure
user> (conj)
[]
user> (+)
0
user> (*)
1
```

These are all good reducing functions for use with `transduce` as they
return an "identity value" of some sort.

Note that previously we said that a reducing function is one that
takes two arguments, a `state` and an `input`, and returns a new
state. That's still true, but now we need to expand our definition. A
proper reducing function that works with transducers includes two
arities. The arity-0 version returns an identity value appropriate for
use as the initial value of a reduction and the arity-2 version
performs a reduction step.

It's important to note that `transduce` and `reduce` have different
behavior in the case where they don't have an initial value. `reduce`
uses the first value of the collection and starts processing at the
second value. This allows you to use functions like `min` or `max` as
a reducing value. But these functions won't work without an initial
value if you try to use them with `transduce`. You can use `first` and
`rest` to pick off the first value if need to.

```clojure
user> (max)
Execution error (ArityException) at user/eval8583 (form-init7109873409143739450.clj:535).
Wrong number of args (0) passed to: clojure.core/max
user> (transduce identity min [1 2 3])
Execution error (ArityException) at user/eval8585 (form-init7109873409143739450.clj:538).
Wrong number of args (0) passed to: clojure.core/min
user> (reduce min [1 2 3])
1
user> (let [coll [1 2 3]]
        (transduce identity min (first coll) (rest coll)))
1
```

Here, for simplicity, I'm just using `identity` as the transducer,
which just returns the reducing function, unchanged.

We can modify our `cc-xd` function to have the same arities and
behavior as the standard `transduce` function.

```clojure
user> (defn cc-xd-2
        ([xf rf coll]
         (cc-xd xf rf (rf) coll))
        ([xf rf init coll]
         (reduce (xf rf) init coll)))
#'user/cc-xd
```

When you consider `transduce`'s arguments, one question that comes up
is why `xf` and `rf` are _separate_ arguments? Why can't we just apply
`xf` to `rf` ahead of time and pass one argument to `transduce`? That
would make the function signature basically the same as `reduce`. In
fact, under limited circumstances, we can do exactly this, and even
pass wrapped reducing functions to `reduce` itself.

```clojure
user> (def filter-odd-times-five-xf (comp (filter odd?)
                                          (map (partial * 5))))
#'user/filter-odd-times-five-xf
user> (def rf (filter-odd-times-five-xf conj)
user> (reduce rf [] (range 10))
[5 15 25 35 45]
```

So, why do it any other way? Wouldn't it be faster and more efficient
to apply the transducer to the reducing function once and then reuse
it possibly multiple times? Yes, it would be marginally faster, but
only by a hair; the dominant portion of any transduction is spent
actually processing the elements.

The reason we do it this way is because the reducing function created
by a transducer is allowed to be _stateful_. Such a transducer is
often called a "stateful transducer" when you read Clojure
documentation about transducers, but as [Christophe Grand pointed
out](https://twitter.com/cgrand/status/509628401918156800) several
years ago, this is not quite accurate. The transducers themselves are
stateless (remember, a transducer simply takes a reducing function and
returns another, wrapped reducing function), _but the new reducing
function returned by the transducer may keep state during the
reduction process._ This state is initialized when the transducer
creates the new reducing function. So, it's better if we create our
transducer stack separately, a stateless operation, and then apply
that to our reducing function right before we perform the
reduction. If we don't do this, then the next time we reduce/transduce
with this reducing function, we'll end up with stale state and things
won't work as expected. The example above worked because the reducing
functions are stateless.

Further, some tranduction contexts choose their own reducing function
and so we'd like to be able to pass around our transducer stack before
it's applied to a reducing function. For instance, `into` can take a
transducer stack and uses Clojure's optimized transient data structure
processing to improve performance. It does not take a separate
reducing function but chooses one based on the output data type.

```clojure
user> (into #{} filter-odd-times-five-xf (range 10))
#{15 25 35 5 45}
```

Examples of standard transducers that create stateful reducing
functions include `drop`, `keep-indexed`, `partition-all`,
`partition-by`, `take`, and `take-nth`. All of these require the
created reducing function to keep track of a count of items seen or an
index of some sort. The doc-strings for transducer creating functions
will specify whether they are stateful or not.

Let's try our hand at writing a transducer that creates a stateful
reducing function. As an exercise, we'll re-write
[`partition-all`](https://clojuredocs.org/clojure.core/partition-all). This
will demonstrate how a reducing function should store state and how to
handle various special cases during the reduction process.

Here's a first attempt. Remember that `partition-all` groups the
processed collection into partitions of `n` items. At the end of the
process, whatever items remain are emitted as their own short
partition.

```clojure
(defn cc-partition-all-1 [n]
  (fn [rf]
    (let [p (volatile! [])]             ; reducing function state
      (fn [state input]
        (let [p' (vswap! p conj input)]
          (if (= n (count p'))
            (do (vreset! p [])
                (rf state p'))
            state))))))
```

You can see the same basic transducer structure that we've seen
before, but here we've wrapped a `let` form containing a binding for a
volatile around the returned reducing function. This is what holds our
state.

The processing is pretty simple. Every time the reducing function is
called, it adds the input item to the saved state, `p`, using
[`vswap!`](https://clojuredocs.org/clojure.core/vswap!), which returns
the new value. We capture this in `p'`. Then if the number of items in
`p'` is equal to the partition size, we reset `p` back to an empty
vector and call the downstream reducing function with the
partition. If the count of elements in the growing partition is not
equal to `n`, we just return the `state`, unchanged.

That's an important new point about transducers: the new reducing
function can control whether it calls the downstream reducing function
or not.

Let's see how it works.

```clojure
user> (cc-xd-2 (cc-partition-all-1 3) conj [] (range 10))
[[0 1 2] [3 4 5] [6 7 8]]
```

Hmmm. Well, that _mostly_ worked. We got partitions of size three. But
we didn't get 10 items in the result like we should have. What's going
on?

The problem here is that when the reduction process is finished, our
`cc-partition-all-1` is still holding onto an item in its state. It
has no way of knowing that the reduction is complete. We need a way to
signal it and say "Hey, we're done here, so perform any clean-up work
you need to." Transducers do this by adding yet another arity to the
reducing function: an arity-1 version.

```clojure
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
```

The arity-1 function includes the logic to clean up. We simply take
the partition that is in process of being formed and if there are any
elements remaining in it, we pass those to the _arity-2_ downstream
reducing function. This adds the final partition to the output. Then
we call the _arity-1_ version of the downstream reducing function on
the new state that we just returned from the arity-2 version. This
signals the downstream reducing function that it should perform any
cleanup in the case that it's stateful as well. If there are no items
in the partition, then we simply pass along the `state` to the
downstream arity-1 version. If you have a reducing function that
doesn't support arity-1, you can easily create a new version that does
by passing it to `completing`. The created arity-1 function will
simply be `identity`.

Now, we also need a new version of our transduction function,
`cc-xd-3`.

```clojure
(defn cc-xd-3
  ([xf rf coll]
   (cc-xd-3 xf rf (rf) coll))
  ([xf rf init coll]
   (let [rf' (xf rf)]
     (rf' (reduce rf' init coll)))))
```

Now things work as expected.

```clojure
user> (cc-xd-3 (cc-partition-all-2 3) conj [] (range 10))
[[0 1 2] [3 4 5] [6 7 8] [9]]
```

So, now we know that a proper reducing function for use with
transducers has arity-0, arity-1, and arity-2 versions.

```clojure
user> (conj)
[]
user> (conj [1])
[1]
user> (conj [1] 2)
[1 2]
user> (+)
0
user> (+ 1)
1
user> (+ 1 2)
3
user> (*)
1
user> (* 1)
1
user> (* 1 2)
2
```

While `transduce` doesn't strictly need it, we should also add an
arity-0 function to our `partition-all` reimplementation:
`cc-partition-all-3`. In this case, the arity-0 function simply
delegates to the downstream reducing function. That's going to be
pretty standard behavior; it's rare that a transducer needs to
manipulate the initial value.

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

Now, we can use our `cc-partition-all-3` with Clojure's standard
transduction functions.

```clojure
user> (transduce (cc-partition-all-3 3) conj [] (range 10))
[[0 1 2] [3 4 5] [6 7 8] [9]]
```

Boom. Nailed it. We have a legitimate transducer.

Okay, that's all for now. Next time, we'll take a look at how to
abort a reduction or transduction right in the middle.
