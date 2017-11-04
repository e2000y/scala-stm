---
layout: default
title: Introduction
---

What is STM? {#whatisit}
------------

Software transactional memory is a mediator that sits between a critical
section of your code (the atomic block) and the program's heap. The STM
intervenes during reads and writes in the atomic block, allowing it to
check and/or avoid interference other threads. If the loads and stores
of multiple threads have gotten interleaved, then all of the writes of
the atomic block are *rolled back* and the entire block is retried. If
the accesses by the critical section are not interleaved, then it is as
if they were done atomically and the atomic block can be *committed*.
Other threads or actors can only see committed changes.

STMs use optimistic concurrency control. They optimistically assume that
atomic blocks will be able to run in parallel, and then back up and
retry if that speculation is incorrect. Keeping the old versions of data
so that you can back up imposes some overhead, but optimistic
concurrency typically has better scalability than alternative
approaches.

ScalaSTM -- no magic {#nomagic}
--------------------

There have been several ambitious attempts to create STMs that can run
existing sequential imperative code in parallel. This is a difficult
task that requires a lot of magic, because calls to the STM need to be
inserted for *every* load and store of a non-final field or array
element inside an atomic block. Good performance is also difficult
because of the large numbers of reads and writes.

The ScalaSTM API avoids the need for magic by only managing `Ref`-s.
This means that there are fewer memory locations to manage, and no
bytecode instrumentation or compiler modifications are required. As in
Haskell and Clojure, the usefulness of `Ref` is multiplied by the
language's good support for immutable data structures. ScalaSTM also
includes concurrent sets and maps that can be used in transactions.

Who is ScalaSTM for? {#whatfor}
--------------------

ScalaSTM is for programmers whose threads or actors need to coordinate
access to shared data. In a server, this might be the list of active
connections or a cache. In a client, this might be a partial result or
worker thread status.

Pros
----

-   **Say what you mean.** You write `atomic`, ScalaSTM executes it
    atomically without deadlocks or races. There is no need to map locks
    to data. Nested atomic blocks do the right thing, so you can build
    complex thread-safe operations from simple ones.


-   **Readers scale.** All of the threads in a system can read data
    without interfering with each other. Optimistic algorithms take
    better advantage of the cache on modern architectures than
    pessimistic approaches.


-   **Exceptions automatically trigger cleanup.** If an atomic block
    throws an exception, all of the `Ref`-s are reset to their original
    state. (You can change this default if you like.)


-   **Waiting for complex conditions is easy.** If an atomic block
    doesn't find the state it's looking for, it can call `retry` to back
    up and wait for any of its inputs to change. If there are multiple
    ways to succeed, you can chain them and ScalaSTM will try them all.


-   **Simple.** ScalaSTM is just a stand-alone library, so it doesn't
    affect the parts of the application that don't use it. This means
    that you can include it inside a framework or hidden component.

Cons
----

-   **Two extra characters per read or write.** If `x` is a `Ref`, then
    `x()` reads its value and `x() = v` writes it.


-   **Single-thread overheads.** In most cases STMs are slower when the
    program isn't actually running in parallel. We've gotten the actual
    costs pretty low, so for most uses this won't be a problem. Rollback
    can be useful even in single-threaded programs to automatically
    clean up on an exception.


-   **Rollback doesn't mix well with I/O.** Only changes to `Ref`-s are
    undone automatically. The ScalaSTM API includes hooks so you can
    perform manual compensation or DB integration, but there is no way
    to recall packets or pixels. Of course, you probably shouldn't be
    doing I/O while you hold a lock, either.
