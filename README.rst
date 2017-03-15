===========
Atomix-test
===========

What is this?
=============

It's a small project to test how atomix works and to demonstrate a couple of problems that I seem to
run into.

Building
========

Just run ./gradlew build and it will build you a fatjar in build/libs/atomixtest-0.0.1-SNAPSHOT.jar

Running
=======

You can run this app like this: `-jar build/libs/atomixtest-0.0.1-SNAPSHOT.jar`. It will read the
configuration from `application.yaml` in the current directory. You can edit to change the location
of the logs, atomix storage as well as number of replicas, and ports they listen on.

application.yaml comes with several configuration profiles that can be used to quickly switch
between different configurations necessary for the tests.

DistributedLong.incrementAndGet().join() blocks indefinitely
============================================================

To run this test, launch three instances of the app like this::

  $ java -jar build/libs/atomixtest-0.0.1-SNAPSHOT.jar --spring.profiles.active=node0
  $ java -jar build/libs/atomixtest-0.0.1-SNAPSHOT.jar --spring.profiles.active=node1
  $ java -jar build/libs/atomixtest-0.0.1-SNAPSHOT.jar --spring.profiles.active=node2

These profiles will configure all instances to increment the same DistributedLong counter with
long delays, and print the returned value.

To reproduce this problem, kill one node with (Ctrl-C for example). Kill another node. Start one of
the killed nodes. Hopefully you will see that a newly started node is able to increment a value,
which means that the copycat itself is running on two nodes, while the nodes that has been left
intact seems to be blocked in a .get(). If it doesn't happen for you on the first attempt,
try again several times, for me it happens every time after several tries.

You can find the logs from all three replicas as well as replica storage files inside of a
`stuck5hrs.tbz2` archive. Inside of the logs you can see all starts/stops of all replicas,
and it was node0 that got stack at this timestamp `2017-03-15 12:34:21.561` and was stuck
for 5 hours until `2017-03-15 17:34:51.249` when it was finally able to complete a new
.incrementAndGet() operation.

At the same time you can see that during all this time node1 was able to successfully perform
.incrementAndGet() which I suppose means that there were two active replicas at the time, i.e.
both node0 and node1 copycat/atomix low-level parts were up and running (node2 was down at the time).
Yet at the same time a client-level .incrementAndGet() call remained blocked for 5 hours.

Obviously I can't say that it would remain blocked forever, but it certainly seems so.

Using .onChange() causes ApplicationExceptions
==============================================

To rin this test, launch three instances of the app like this::

  $ java -jar build/libs/atomixtest-0.0.1-SNAPSHOT.jar --spring.profiles.active=node0
  $ java -jar build/libs/atomixtest-0.0.1-SNAPSHOT.jar --spring.profiles.active=node1
  $ java -jar build/libs/atomixtest-0.0.1-SNAPSHOT.jar --spring.profiles.active=node2-listen

These profiles will configure all instances to run an atomix replica with a DistibutedLong counter,
the same as in the previous test, but node2 will create will put an .onChange() listener instead of
calling an increment.

First let it run for a while, and observe that everything works as expected: node0 and node1 are
able to increment the counter and report new values, while node2 receives updates on all counter
changes. Now kill node2, after several seconds you should start seeing ApplicationExceptions
in both node0 and node1. At this point restarting node0 and node1 won't help. It seems that
something wrong has been committed into the storage, that causes this.

You can find logs from all the replicas as well as replica storage files inside of a
`application-error.tbz2` archive.

Eveything works fine
====================

It's not really the point of this demo, but if you want to see everything working smoothly
you can start nodes like this::

  $ java -Datomix.incrementTimeoutSec=5 -jar build/libs/atomixtest-0.0.1-SNAPSHOT.jar --spring.profiles.active=node0
  $ java -Datomix.incrementTimeoutSec=5 -jar build/libs/atomixtest-0.0.1-SNAPSHOT.jar --spring.profiles.active=node1
  $ java -Datomix.incrementTimeoutSec=5 -jar build/libs/atomixtest-0.0.1-SNAPSHOT.jar --spring.profiles.active=node2

