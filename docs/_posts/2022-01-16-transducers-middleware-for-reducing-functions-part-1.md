---
layout: post
title: "Transducers: Middleware for Reducing Functions (Part 1)"
date: 2022-01-16 9:00:00 -0600
---
This is a four-part series. You can find the parts here:
* [Part 1](/clojurecrazy/2022/01/16/transducers-middleware-for-reducing-functions-part-1.html)
* [Part 2](/clojurecrazy/2022/01/17/transducers-middleware-for-reducing-functions-part-2.html)
* [Part 3](/clojurecrazy/2022/01/18/transducers-middleware-for-reducing-functions-part-3.html)
* [Part 4](/clojurecrazy/2022/01/19/transducers-middleware-for-reducing-functions-part-4.html)

In our [last
episode](/clojurecrazy/2022/01/10/using-reduce-to-implement-other-clojure-functions.html),
we learned how we can re-implement many other Clojure collection
processing functions such as `mapv` and `filterv` in terms of
`reduce`. Were you able to implement some of the other functions we
talked about like `keep` or `group-by`?

In this post, we'll start to look into Clojure _transducers_. There is
a lot of material, so we won't get through everything today, but we'll
continue next time.

Now, for whatever reason, transducers are to Clojure what monads are
to Haskell: they both take a bit of a mind warp to understand and they
have both spawned a cottage industry of tutorials across the
Internet. Well, never wanting to be left out, this is my transducer
tutorial. Unlike some who have compared transducers to burritos, I'm
going in a different direction.

All the monad tutorials make it easy for you to understand monads by giving
you a succinct description of them. "A monad is just a monoid in the
category of endofunctors," they say. Yes, yes, of course. That much is
obvious.

I've got my own pithy description of transducers, too: Transducers are
just middleware for reducing functions. There. See. That was
easy. "Alright," I hear you saying, "That much is obvious, but could
you unpack that a bit?" Of course.

Let's start by remembering why transducers exist in the first place.

First, Clojure's original collection processing functions were
lazy. Calling `map` or `filter` on a collection doesn't actually do
all the processing at the time of the initial call. Rather, these
functions return a lazy-seq that delays processing until you actually
consume the results. This is fine if you're always dealing with
infinite sequences and wanting to only realize a small set of results,
but it's inefficient if you always want to process the whole
collection. In contrast, transducers are greedy and process the whole
collection at once.

Now, there are other functions that we saw last time that also are
greedy, such as `mapv` and `filterv`. If you want performance, they
work great. Unfortunately, they have one drawback: when you compose
them together, they create multiple intermediate vectors that are
immediately thrown away. For instance:

```clojure
user> (->> (range 10)
           (filterv odd?)
           ;; there's an intermediate sequence here
           (mapv (partial * 5)))
[5 15 25 35 45]
```

In this case, `filterv` creates an output vector which is consumed by
`mapv` which then creates another output vector. That intermediate
vector is short-lived and just creates more garbage for the runtime GC
to deal with.

Transducers allow us to compose `filter` and `map` transformations
without creating intermediate garbage.

Finally, `map` and `filter` always create an output sequence (because
it's a lazy-seq) and `mapv` and `filterv` always create output
vectors. But what if you want to process your collection and return a
set as the output? You could use `into`, like so.
```clojure
user> (->> (range 10)
           (filterv odd?)
           ;; there's an intermediate sequence here
           (mapv (partial * 5))
           ;; and another one here
           (into #{}))
#{15 25 35 5 45}
```

But this creates yet another intermediate sequence (the output from
`mapv`) that is just thrown away, increasing GC pressure even further.

Transducers are independent of the final collection processing. You
can use the same transducer stack to output a vector or a set or
whatever.

Okay, so _what is_ a transducer? As I said, a transducer is middleware
for reducing functions. Let's talk about middleware for a bit.

If you're familiar with Clojure's popular web application library,
Ring, then you're familiar with middleware. If not, the concept is
fairly simple.

In Ring, a web request is parsed by the server into a Clojure map that
contains information about the request. Each request is ultimately
fulfilled by a handler. A handler is just a Clojure function that
takes a request map as input and returns another map that describes
the response. The web server then uses the response map to format an
HTTP response back to the client.

The [Ring
wiki](https://github.com/ring-clojure/ring/wiki/Getting-Started) gives
an example of a simple hander:

```clojure
(defn handler [request]
  {:status 200
   :headers {"Content-Type" "text/html"}
   :body "Hello World"})
```

This handler just ignores the request and returns a "Hello World"
response.

Ring's concept of middleware allows you to wrap a handler with another
function that does pre-processing or post-processing of the request. Again, the
Ring wiki provides this simple example:

```clojure
(defn wrap-content-type [handler content-type]
  (fn [request]
    (let [response (handler request)]
      (assoc-in response [:headers "Content-Type"] content-type))))
    
(def app
  (wrap-content-type handler "text/html"))
```

So, you can see that `wrap-content-type` returns a function closure
that wraps the `handler` argument. This function closure is then
assigned to `app` which is passed to the web server as the
handler. The function clojure returned by `wrap-content-type` first
calls the original `handler` and then modifies the `response` map on
the way out. The middleware can also modify the request map before
calling the wrapped handler.

You can apply a series of Ring middleware easily:

```clojure
(def app
  (-> handler
      (wrap-content-type "text/html")
      (wrap-keyword-params)
      (wrap-params)))
```

Here, we're passing the handler to the `wrap-content-type` middleware,
which returns a closure, which is passed to the `wrap-keyword-params`
middleware, which returns another closure wrapping the first, which is
then passed to the `wrap-params` middleware that finally returns
another closure which wraps the others. So, when a request comes in,
it's going to get passed first to `wrap-params`, then
`wrap-keyword-params`, then by `wrap-content-type`, and finally by the
original handler. Any of those pieces of middleware can update the
request map on the way in and the response map on the way out.

Now, let's take a look at our custom version of `into`.

```clojure
user> (defn cc-into [to from]
        (reduce conj to from))
#'user/cc-into
```

Note that we're using `conj` as our reducing function here to build
the final output collection.

Now, let's take a look at our custom version of `filterv`.

```clojure
user> (defn cc-filterv [pred coll]
        (reduce (fn [state input]
                  (if (pred input)
                    (conj state input)    ; here's the conj
                    state))
                []                        ; here's the output collection
                coll))
#'user/cc-filterv
```

Notice how this works. The reducing function in `cc-filterv` applies
the test predicate to the input and if the result is truthy, _it calls
another reducing function_, `conj`. The `conj` and the initial empty
vector are there to build the output collection. 

If you look at this abstractly, we've wrapped some middleware,
hardcoded to perform filtering, around the final reducing function
which builds the output value. The `conj` is playing the same role as
the final `handler` in the case of Ring. We're not quite at the stage
where this is a transducer, but this is where we're heading.

Interestingly, `cc-filterv` only creates vectors as the output
collection. What if we wanted to build another type of collection, or
even a whole new data structure, after filtering the input collection?
Certainly, we could always compose `cc-filterv` with another function.

For instance, maybe we want to filter map entries.

```clojure
user> (defn map-value-odd? [[k v]]
        (odd? v))
#'user/map-value-odd?
user> (->> {:a 1 :b 2 :c 3 :d 4}
           (cc-filterv map-value-odd?)
           (cc-into {}))
{:a 1, :c 3}
```

This works great, but we're creating an intermediate collection
again. Ideally, we could do it in one step. If we modify
`cc-filterv` a bit, we can get the behavior we want. Here's
`cc-filter-into`. It can filter a collection and put it into another
output collection using a supplied reducing function in a single pass
over the input collection.

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
```

Bingo! One function that filters an input collection and builds an
output value of our choice using a final reducing function that we
specify. No intermediate collections. In fact, since we have control
of the reducing function and the initial value, the final value
doesn't even have to be a collection (in this sense, `cc-filter-into`
is a poor name as it implies the final result is a collection).

For instance, we could sum up the odd values in a set.
```clojure
user> (cc-filter-into odd? + 0 #{1 2 3 4 5 6 7 8 9})
25
```

This is awesome!

"Hmmm," I hear you say. "But what if we want to do something other
than just filtering? Maybe we want to modify each value, like we would
with `map`. This seems cool, but it's a bit limiting."

That's a great question. Unfortunately, we're out of words for today,
so we'll hold it until next time.
