---
layout: post
title: "My Experience with an M1 MacBook Air"
date: 2022-01-30 18:22:00 -0600
---

Just before Christmas last year, I decided to treat myself to a
present. My personal laptop was an old, 2017, 16-inch MacBook Pro that
whose battery was failing and no longer held a charge for more than a
couple of hours. I found myself over the preceding months craving one
of the slick, new M1 Apple Silicon models. In 2020, I had watched the
Apple announcements with anticipation. The M1 chip looked really slick
and fast. But I decided that I would wait for the second generation
models, as you never know whether the first generation of anything
will need a few kinks worked out before it's fully ready for
primetime.

In April, my son needed a laptop for school, so I bought him a base
model M1 Air to see how it performed and whether he experienced any
problems. Everything seemed fine. So, at least for basic office
productivity apps and email, it was a good machine.

In September, my company decided to upgrade my work laptop to a
13-inch, M1-based MacBook Pro. Over the next couple months, I found
the machine to be quite capable and speedy for everyday office tasks
(email, Microsoft Office, etc.). My confidence was building. Further,
the work laptop was an 8 GB RAM base model. While memory certainly
gets tight sometimes with my corporate app workload, the M1 and SSD
are so fast at swapping that you rarely notice. Still, I wish I had 16
GB on that machine.

In October, my daughter spilled a glass full of juice on the keyboard
of my wife's old x86-based MacBook Air. After a trip to the Apple
store, it was pronounced dead and so we buried it and bought her a
new, base-model M1 Air. It worked out great for her. Again, no
compatibility issue or complaints about something not working. Another
positive data point.

So, in late December, I decided to take the plunge and upgrade my own
machine to one of the new Apple Silicon models. The first quesstion
was, which model should I get? I watched all the YouTube reviews of
the new M1 Pro and M1 Max models. While they both seemed fast, it felt
like they were targeting media professionals working with video more
than anything else. Further, reports were that the laptops were
heavy. I loved the 16-inch screen, but I wanted something light, with
all-day battery life, if possible. They were also a lot more
expensive, with the models I would have chosen running nearly $2500.

More and more, I found myself drawn to the year-old, M1 Air. It's
small, light, and relatively inexpensive. I figured if there was
something I found out about it after purchasing it that I really
hated, I could always dispose of it on eBay and I wouldn't be out that
much money in any case. By the time I made the decision, Apple was
already starting to offer the M1 Air as part of their refurb
program. So, I saved myself a few dollars and bought a refurbished M1
Air with some upgraded specs: 16 GB of RAM and 1 TB of SSD. That's
basically 2x the RAM and 4x the storage of the base model. And the
price was $800 lower than I would have paid for a 14-inch MacBook Pro
with M1 Pro processor.

After I received it, I installed the latest Emacs 28 pretest from
[emacsformacosx.com](https://emacsformacosx.com/builds). I also
installed Azul's [Zulu VM
(JDK17)](https://www.azul.com/downloads/?package=jdk) via
[Homebrew](https://brew.sh). Both of these builds have native M1
support, so I'm not running under Apples Rosetta, x86 compatibility
layer.

So, how does it work? In a word, great. The machine is small and
light. The battery does last all day, even when I'm browsing the web
and watching YouTube. The four high-efficiency cores sip power when
the machine is just sitting there waiting for keystrokes. The four
high performance cores kick into action when I run a sustained
workload. And the performance cores are definitely faster than my
previous, four-year-old x86 based MacBook Pro. Running some
CPU-intensive Clojure programs that I have written, I estimate double
the speed. Without a fan, the machine runs very cool. You can really
put it on your lap and use it while sitting on your couch without your
thighs burning. The keyboard is awesome. Much, much, much better than
my MBP-16 with the old "butterfly keyboard," Apple's worst keyboard
ever. I have been running 50 tabs in Safari, a couple of Microsoft
Office apps, Evernote, Slack, Emacs, and a couple of Java processes,
one a Clojure program I use for personal finance and another connected
to Emacs with CIDER. With all that going on, my memory usage is only
12 GB and I've only used 57 MB of swap. Everything flies.

If you want to use it to develop in Clojure, you can do so with
ease. That's really a testament to using a high level programming
language (Clojure) running on an abstract virtual machine (JVM) that
totally insulates you from the hardware details. I also haven't gone
as far as trying Docker or anything virtualized (e.g., VMware), so you
might experience some level of pain there. That's not something I
typically need to do.

Are there downsides? Yes, two, but they are very minor in my
opinion. First, the screen size is only 13 inches. I would really like
a full 16-inch model. If the rumors are true, this might be coming to
the next generation of MacBook Air later this year, along with M2 or
M1 Super Extreme Pro Bionic, or whatever Apple's marketing department
decides to name the next generation chip.

The second downside is that because the Air doesn't have a fan, if you
run CPU-intensive workloads for a sustained period of time, the
thermal management system will kick on and reduce CPU frequency to
keep the system within the thermal design envelope. But I don't
typically run workloads like that. Mostly, I run event-driven
workloads where the programs are waiting for keystrokes or network
packets or whatever. But even when I do, those CPU-intensive workloads
typically run for less than five minutes and then they're done.  So, I
haven't noticed any slowdowns whatsoever from thermal throttling. But
if you're doing something that hammers the CPU without end like video
processing, you would probably be better off with a MacBook Pro with a
fan.

Overall, the experience has been great. Everything from Emacs to Java
to Clojure has just worked with little to no fiddling other than
making sure I chose M1-native versions (e.g., the Emacs 28 pretest
builds). Even when something runs under Rosetta, it's fast and
transparent. Without looking specifically in Activity Monitor, I never
really know whether some software is M1-native or not. It's just that
fast. The only real complaint I have is that I'd like those three extra
inches of screen size to fit more windows and text on screen at one
time. Other than that, it's all good.

So, if you've been thinking about getting an Apple Silicon machine in
general, or an M1 MacBook Air in particular, I can tell that they run
great. Take the plunge.
