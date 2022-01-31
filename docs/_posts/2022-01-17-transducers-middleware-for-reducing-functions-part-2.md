---
layout: post
title: "Transducers: Middleware for Reducing Functions (Part 2)"
date: 2022-01-17 9:15:00 -0600
tags: clojure.core transducers reduce
---
This is a four-part series. You can find the parts here:
* [Part 1](/clojurecrazy/2022/01/16/transducers-middleware-for-reducing-functions-part-1.html)
* [Part 2](/clojurecrazy/2022/01/17/transducers-middleware-for-reducing-functions-part-2.html)
* [Part 3](/clojurecrazy/2022/01/18/transducers-middleware-for-reducing-functions-part-3.html)
* [Part 4](/clojurecrazy/2022/01/19/transducers-middleware-for-reducing-functions-part-4.html)

[Last time](/clojurecrazy/2022/01/16/transducers-middleware-for-reducing-functions-part-1.html), we learned that transducers are just middleware for
reducing functions. In the same way that Clojure's Ring web
application library uses middleware to wrap HTTP handlers, transducers
are used to wrap and modify reducing functions.

When we left off, we had just designed a new function,
`cc-filter-into`, that acts a lot like Clojure's standard `filterv`
function, but allows the caller to specify the final reducing function
that builds the output as well as an initial value.

```clojure
user> (defn cc-filter-into [pred rf init coll]
        (reduce (fn [state input]
                  (if (pred input)
                    (rf state input)
                    state))
                init
                coll))
#'user/cc-filter-into
user> (cc-filter-into map-value-odd? conj {} {:a 1 :b 2 :c 3 :d 4})
{:a 1, :c 3}
user> (cc-filter-into odd? + 0 #{1 2 3 4 5 6 7 8 9})
25
```

You had just made the comment, "But what if we want to do something other
than just filtering? Maybe we want to modify each value, like we would
with `map`. This seems cool, but it's a bit limiting."

What we want to do is pull out the hard-coded filtering
logic. Instead, we're going to pass the reducing function to a
middleware function that will wrap the reducing function with the
logic we need. This is right where we start to see transducers come
in. A transducer is a function that takes a reducing function and
returns a new reducing function that applies some logic to the one it
was passed.

Let's start by creating a transducer that implements our filtering
logic.

```clojure
user> (defn cc-filter-xf [pred]    ; this is the transducer constructor
        (fn [rf]                   ; this is the transducer
          (fn [state input]        ; this is the new reducing function wrapper
            (if (pred input)       ; here's our filtering logic
              (rf state input)
              state))))
#'user/cc-filter-xf
```

We have multiple levels of functions returning functions here, so
let's unpack this step by step. The outer function, named
`cc-filter-xf`, is what I call a _transducer constructor_. It's not a
transducer itself, but it creates a transducer. Specifically, it's
used to bind other variables that our filtering logic will need later
in a closure. In this case, the `pred` argument is wrapped in this
closure. We need to use that when we do the actual filtering. The
`cc-filter-xf` function returns an anonymous function that takes a
reducing function and returns another reducing function. This is the
actual transducer. It's the middlware that wraps the reducing function
and adds the logic we want. The new reducing function takes the
`state` and `input` from `reduce`, performs the logic it needs to, and
calls `rf`, the original reducing function.

Now, we can make our own simple version of Clojure's `transduce`.

```clojure
user> (defn cc-xd [xf rf init coll]
        (reduce (xf rf) init coll))
#'user/cc-xd
```

We apply the transducer, `xf`, to the reducing function, `rf`, right
before we call `reduce` on the other parameters. The transducer wraps
all of our logic up in a closure that act as middleware around the
reducing function.

```clojure
user> (cc-xd (cc-filter-xf odd?) conj [] (range 10))
[1 3 5 7 9]
```

Boom.

Let's try to make another transducer. This time, let's do a mapping
transducer. Remember our `cc-mapv` function from 
"[Using `reduce` to Implement Other Clojure Functions](/clojurecrazy/2022/01/10/using-reduce-to-implement-other-clojure-functions.html)?"

```clojure
user> (defn cc-mapv [f coll]
        (reduce (fn [state input]
                  (conj state (f input)))
                []
                coll))
#'user/cc-mapv
```

Here's an equivalent transducer.

```clojure
user> (defn cc-map-xf [f]            ; this is the transducer constructor
        (fn [rf]                     ; this is the transducer
          (fn [state input]          ; this is the new reducing function
            (rf state (f input)))))  ; here's where we apply f to the input
#'user/cc-map-xf
user> (cc-xd (cc-map-xf inc) conj [] (range 10))
[1 2 3 4 5 6 7 8 9 10]
user> (cc-xd (cc-map-xf (partial * 5)) conj [] (range 10))
[0 5 10 15 20 25 30 35 40 45]
```

Now, here's the part that's really great. It's easy to _compose_
transducers just like Ring middleware.

```clojure
user> (def filter-odd-times-five (comp (cc-filter-xf odd?)
                                       (cc-map-xf (partial * 5))))
#'user/filter-odd-times-five
user> (cc-xd filter-odd-times-five conj [] (range 10))
[5 15 25 35 45]
```

Here, we've filtered out non-odd numbers and we've multiplied each of
them by five. But there are no intermediate collections involved! Each
item in the source collection is processed only once. And we can also
control the return value and final processing using the reducing
function (`conj`) and the initial value (an empty vector).

Remember that `comp` composes functions in the opposite order than the
threading macros. So we had the example last time of a series of Ring
middleware being applied to a handler.

```clojure
(def app
  (-> handler
      (wrap-content-type "text/html")
      (wrap-keyword-params)
      (wrap-params)))
```

We said that in this case, the handler is being wrapped by
`wrap-content-type` first, then `wrap-keyword-params` second, and
finally `wrap-params` third. This means that during execution,
`wrap-params` gets called first, which then calls
`wrap-keyword-params`, which then calls `wrap-content-type`, which
then calls the `handler`.

In our case, we're creating a composed transducer using `comp`, so the
order is reversed. Now, `cc-filter-xf` is the outer wrapper
and the reducing function created by its transducer is called first,
which then calls the reduction function created by `cc-map-xf`'s
transducer second, which then finally calls the reducing function
passed to the composite transducer in `cc-xd`.

Clojure's standard collection functions will act as transducer
constructors when you leave off the collection. We can even use those
transducers with our `cc-xd` function, though we can't yet use our
primitive transducers with the standard `transduce` function.

```clojure
user> (cc-xd (comp (filter odd?)
                   (map (partial * 5)))
             conj
             []
             (range 10))
[5 15 25 35 45]
```

Okay, that's all for now. In the next post, we'll try our hand at
writing some actual transducers. We'll also learn how to deal with
transducers that return stateful reducing functions.

