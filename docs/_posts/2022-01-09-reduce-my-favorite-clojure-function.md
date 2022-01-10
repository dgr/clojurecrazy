---
layout: post
title: "Reduce: My Favorite Clojure Function"
date: 2022-01-09 19:30:00 -0600
---
I have to admit it, I have a favorite Clojure function. No, it's not
[`map`](https://clojuredocs.org/clojure.core/map), although that would
be a close second. No, it isn't
[`juxt`](https://clojuredocs.org/clojure.core/juxt), though that's
certainly one of the fun
ones. [`partition`](https://clojuredocs.org/clojure.core/partition)?
Juicy, but no. [`assoc`](https://clojuredocs.org/clojure.core/assoc)?
Nope. [`str`](https://clojuredocs.org/clojure.core/str)?
Nyet. [`fnil`](https://clojuredocs.org/clojure.core/fnil)? Nein.

My favorite Clojure function is
[`reduce`](https://clojuredocs.org/clojure.core/reduce). Why `reduce`?
Let me count the ways...

`reduce` is one of those bedrock functions that shows up in so many
functional programming contexts that it's just ridiculous. I would go
so far as to say that if you don't understand `reduce`, you don't
understand functional programming. In other functional languages,
you'll find it named
[`fold`](https://fsharp.github.io/fsharp-core-docs/reference/fsharp-collections-listmodule.html#fold)
or [`foldl`](https://wiki.haskell.org/Fold).

The simple definition of `reduce` is that it takes a _reducing
function_, an _initial value_ (optional), and an _input collection_. It
then calls the reducing function on the initial value and the first
item of the collection. It then calls the reducing function on the
return value from the first call and the second item of the
collection, and so on, finally returning the value returned from the
last call to the reducing function.

The canonical first example of using reduce is:
```clojure
user> (reduce + 0 [1 2 3 4 5])
15
```

Here, `+` is the reducing function, `0` is the initial value, and `[1
2 3 4 5]` is the collection.

You can use `reduce` to compute lots of interesting things over a
larger collection. For instance, Clojure's `min` and `max` functions
work on just two arguments, but you can use them over a collection
like this:
```clojure
user> (reduce max [0 1 17 2 85 43 21 3 10])
85
user> (reduce min [0 1 17 2 85 43 21 3 10])
0
```

Here, we have not provided `reduce` with an initial value, so it uses
the first value in the collection as the initial value and starts
feeding items from the collection with the second value. In this case,
that's just what we want since there's really no appropriate minimum
or maximum value to act as the initial value.

The name `reduce` implies that the collection is being reduced into
some sort of "smaller" value, as above, but that isn't required
required. We can actually use `reduce` to build up a structure that is
much larger than the input. Because of that, I actually think `fold`
is a better name than `reduce`. The name `fold` implies that each of
the successive values in the input collection is being folded into the
accumulated state, much like ingredients in a baking recipe are folded
into a dough of some sort. Sometimes, the accumulated state gets
smaller; other times it gets larger.

For instance, we can return a vector containing all the prefixes of
the input collection like this:

```clojure
user> (reduce (fn [state input]
                (conj state ((fnil conj []) (last state) input)))
              []
              [1 2 3 4 5])
[[1] [1 2] [1 2 3] [1 2 3 4] [1 2 3 4 5]]
```

Here, `reduce` is building up the state; the longer the input
collection, the larger the result.

Sometimes, the first argument to the reducing function is called the
_accumulator_ and is often named `a` or `acc`, but I tend to think of
it more generally as the _current state_ of the reduction, and name
it `state` in my examples. Once you make that shift, it'll open up
your mind to see other places where you can use `reduce`.

For instance, here's a little automaton that looks for two successive
instances of `:b` within in the collection:

```clojure
user> (reduce (fn [state input]
                (case state
                  :start (case input
                           :a :saw-a
                           :b :saw-b)
                  :saw-a (case input
                           :a :saw-a
                           :b :saw-b)
                  :saw-b (case input
                           :a :saw-a
                           :b :saw-b-twice)
                  :saw-b-twice :saw-b-twice))
              :start
              [:a :b :a :b :b :a :a :a :a])
:saw-b-twice
```

In this case, the `state` just holds a Clojure keyword and we bounce
among states according to the current `state` and the `input`. The
reducing function acts as the transition function for the
automaton. You could obviously create a more sophisticated way to
build a larger state transition function (nested `case` forms are only
readable up to a point).

There are two key points that you should understand about `reduce`
that make it so useful and ultimately powerful in other contexts.

The first key point is that `reduce` is valuable whenever you have a
sequence of values and you need to transform or update a piece of
state according to that sequence. That state can be something small
that you just carry along from one call of the reducing function to
the next (e.g., `+`, `min`, `max`, or the automaton) or a large data
structure that you're building up (e.g., our vector of
prefixes). Semantically, `reduce` keeps track of that state value and
_folds_ in each of the items in the collection.

The second key point is that there are many functions that can serve
as reducing functions. Some are those in the Clojure standard library
(e.g., `+` or `max`). Others are custom (e.g., the automaton state
transition function). The only requirement for a reducing function is
that it takes the _state_ as its first parameter and an _input_ as the
second and returns a newly updated _state_. That's it. There's no
requirement that the new state even be the same type from call to
call.

For instance, here's a silly reducing function that just ignores the
state and returns the input:
```clojure
user> (reduce (fn [state input] input) [:a 1 "foo"])
"foo"
```

Since `"foo"` is the last item in the collection, it's the last thing
returned from the reducing function and the result of the `reduce`.

So, that's why `reduce` is my favorite Clojure function. It's just
tremendously useful for so many different tasks. It's a general
workhorse for data processing and can be applied to many different
problems in many different situations.

Next time, we'll see some of those other problems. In particular,
we'll try writing some other standard Clojure functions using
`reduce`. This will set the stage for a deeper understanding of how
Clojure _transducers_ work. Until then, happy reducing!

