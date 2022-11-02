Vector API powered by Panama Project
===================

Project architecture
-----------

This project distinguishes between two types of vectors:
 - On heap vectors represented by all classes implementing the abstract class `AArrayVector`
 - Off heap vectors represented by all classes implementing `AFixedBlockVector`

By contrast to on heap vectors, off heap (or direct) vectors are bound to a block of direct memory reserved using 
a dedicated allocator.
Currently, the project features a reference direct memory allocator for storing data off heap: `UnsafeNativeMemoryAllocator`.
Though implemented in a straight-forward way, it still has several underlying chunks depending
on the size of inner blocks it allocates.<br>
The interface `MemoryAllocator` is the specification for any allocator tested in this project.

This allocator is used to implement `Chunk`s, that represent an array of raw data. ActiveViam having
goals to get the best of the machine performance, Chunks cannot have generic methods, not to pay
the cost of boxing/unboxing. That's the main reason for the existence of `Chunk`. It provides
read and write methods adapted for primitive types. In this scholar project, we only define methods
for `int` and `double`.

Development environment
-----------

This project is written in Java 19, though it does not use many of the latest features so far. Particularly,
implementation depending on project Panama features are to be implemented by the students.

It uses Maven to manage its dependencies.

### JDK

Download JDK 19 from [https://jdk.java.net/19/](https://jdk.java.net/19/). When working for multiple
java version, it's advised to use tools for version management. [SDK Man](https://sdkman.io/) is one of the most common one.

### Compiling the project

```bash
mvn compile
```

Build print compilation warnings due to the usage of `sun.misc.Unsafe`. This is indeed an internal
API but it has been accepted by many vendors. [Project Panama](https://github.com/openjdk/panama-foreign/)
aims at providing safer alternatives, but there are not ready yet.

### Running unit tests

Testing f

```bash
mvn test
```

Tasks
-----------

2. [x] Implement a simple In-memory database (Mahieddine)
3. [ ] Implement vector operations using Panama API
5. [ ] Use Panama foreign Memory API for direct memory allocation
4. [ ] Benchmark your code (can be done while implementing the prototype)
* Use [JMH](https://www.supinfo.com/articles/single/9474-microbenchmarking-java-avec-jmh)
  (steps to define by Mahieddine)
5. [ ] Create new prototypes of Vector API
