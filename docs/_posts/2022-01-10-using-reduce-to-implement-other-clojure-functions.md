---
layout: post
title: "Using `reduce` to Implement Other Clojure Functions"
date: 2022-01-10 17:00:00 -0600
---
In
[yesterday's](https://dgr.github.io/clojurecrazy/2022/01/09/reduce-my-favorite-clojure-function.html)
post, we took a look at `reduce`, one of the true workhorses of
functional programming. In today's post, we'll see how we can use
`reduce` to implement some other functions in the Clojure standard
library.

As we learned previously, `reduce` takes three arguments: a _reducing
function_, an optional _starting value_, and a _collection_. If you
don't supply `reduce` with a starting value, it will use the first
item in the collection as the starting value and start processing from
the second item.

We also learned that a reducing function takes two arguments. The
first argument is the current state of the reduction, and the second
is the input value to be used to compute the new state, which is the
return value. In yesterday's post, I said that there are other Clojure
standard library functions that can act as a reducing function. One of
the most important is `conj`.

Often, we think of `conj` as just adding an item to a vector, like so
```clojure
user> (conj [1 2] 3)
[1 2 3]
```

But `conj` also works on other data types such as maps and sets.

```clojure
user> (conj {:a 1} [:b 2])
{:a 1, :b 2}
user> (conj #{:a :b} :c)
#{:c :b :a}
```

It turns out that `conj` is also a reducing function. It takes a
collection (the state) as the first argument and an item to add to the
collection (the input) as its second argument.

We can use `reduce` with `conj` as our reducing function to create our
own Clojure Crazy version of `into`.
```clojure
user> (defn cc-into [to from]
        (reduce conj to from))
#'user/cc-into
user> (cc-into {:a 1 :b 2} [[:c 3]])
{:a 1, :b 2, :c 3}
user> (cc-into #{} [1 2 3 4 5 6])
#{1 4 6 3 2 5}
```

It's fairly easy to re-implement other Clojure standard functions,
too. Here's a re-implementation of `filterv`, which is basically just
a non-lazy version of `filter` which returns its results in a vector
instead of a lazy sequence.
```clojure
user> (filterv odd? (range 10))
[1 3 5 7 9]
user> (defn cc-filterv [pred coll]
        (reduce (fn [state input]
                  (if (pred input)
                    (conj state input)
                    state))
                []
                coll))

#'user/cc-filterv
user> (cc-filterv odd? (range 10))
[1 3 5 7 9]
```

Here, our reducing function applies the predicate to the input value
and if it returns "truthy," the reducing function adds the input to
the state via `conj`. If the predicate returns "falsey" (either `false`
or `nil`), then the reducing function returns the `state`
unchanged. This has the effect of not including that particular `input`
item in the output vector.

Finally, here's a re-implementation of `mapv`.
```clojure
user> (defn cc-mapv [f coll]
        (reduce (fn [state input]
                  (conj state (f input)))
                []
                coll))
#'user/cc-mapv
user> (cc-mapv (partial * 5) (range 10))
[0 5 10 15 20 25 30 35 40 45]
```

It's amazingly simple. Our reducing function applies `f` to every
`input` and then adds the result to the `state` using `conj`. That's
it.

We could go on and re-implement similar replacements for lots of other
collection functions such as `distinct`, `group-by`, `keep`, `remove`,
`replace`, etc. They're all variations on this. If you want some
exercises to help these ideas really stick, try implementing them all
yourself. Start with `keep` and `remove`. Then, move on to `distinct`,
`group-by`, and `replace`.

It's important to note that all of our replacement functions have been
"greedy" like `mapv` and `filterv`, not "lazy" like `map` or
`filter`. According to its nature, `reduce` processes the full input
collection before it returns a result.

Also, we've used `conj` to build the output vector since it adds items
to the back of the vector, not the front like `cons`. Thus, it
preserves the order of the items in the collection as it processes
them. If you implement a replacement for `group-by`, you'll need to
think about using functions other than `conj` to build up the output
value.

Next time, we'll see how `reduce` and these ideas about
re-implementing functions like `filterv` and `mapv` using `reduce`
start to set the stage for understanding Clojure transducers.
