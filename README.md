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

1. [x] Implement a simple In-memory database (Mahieddine)
2. Panama exploration
 * [ ] Check if we can load off-heap vectors directly into panama vector API (Foreign Memory API [example](see https://openjdk.org/jeps/424)) (students)
 * [ ] Create a dedicated off heap allocator using Segments (students)
 * [ ] Write test suite for vector operations (mahieddine) 
 * [ ] Rewrite `DirectDoubleVectorBlock` and `DirectIntegerVector` operations using panama API (students)
 * [ ] Write a `DirectMemoryAllocator` using `MemorySegments` (students). It will be nice to compare allocation performance between the new allocator and the `Unsafe` allocator.
3. [ ] Benchmark your code (can be done while implementing the prototype)
 * Use [JMH](https://www.baeldung.com/java-microbenchmark-harness). Additional, materials can be found on official [JMH page](https://github.com/openjdk/jmh).
 * Use [JITWatch](https://github.com/AdoptOpenJDK/jitwatch) to analyze the HotSpotJIT compiler.
 * For further analysis, one can use Intel [VTune profiler](https://www.intel.com/content/www/us/en/develop/documentation/vtune-help/top/analyze-performance/code-profiling-scenarios/java-code-analysis.html). Careful,
it works only with Oracle or OpenJDK and on Linux only.
 * Virtual azure machine with SIMD instruction and benchmark template.
4. [ ] Create new prototypes of Vector API

Useful Links
-----------

* Enable [JIT compilation logs](https://www.baeldung.com/jvm-tiered-compilation) using `-XX:+PrintCompilation`
* Print assembly code for bytecoded and native methods using `-XX:PrintAssembly` and `-XX:CompileCommand=print` [documentation](https://wiki.openjdk.org/display/HotSpot/PrintAssembly).
* There are two additional documents for debugging tips in the documentation folder.
* [JIT loop unrolling, auto-vectorization vs Vector API](https://medium.com/@Styp/auto-vectorization-how-to-get-beaten-by-compiler-optimization-java-jit-vector-api-92c72b97fba3)