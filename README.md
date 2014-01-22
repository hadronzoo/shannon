# Shannon [![Build Status](https://travis-ci.org/hadronzoo/shannon.png?branch=master)](https://travis-ci.org/hadronzoo/shannon)

Arithmetic coding library for Clojure and Clojurescript.

## Artifacts

`shannon` artifacts are
[released to clojars](https://clojars.org/com.joshuagriffith/shannon).

If you are using Maven, add the following repository definition to
your `pom.xml`:

```xml
<repository>
  <id>clojars.org</id>
  <url>http://releases.clojars.org/repo</url>
</repository>
```

Or with Leiningen:

```clj
[com.joshuagriffith/shannon "0.1.1"]
```

## Usage

Shannon registers default polymorphic compressors for most standard
Clojure and Clojurescript types:

```clj
(use 'shannon.compressor)

(def out (compress [1 2 3 {:a :b}]))
```

`compress` will return a `ByteArray` in Clojure and a `#js [...]`
array of unsigned bytes in Clojurescript. To decompress:

```clj
(decompress out) ; ⇒ [1 2 3 {:a :b}]
(count out)      ; ⇒ 11 (bytes)
```

Shannon also supports coding with several discrete distributions. For
example, to create a
[Zipf-distributed](http://en.wikipedia.org/wiki/Zipf's_law) coder for
the first million integers:

```clj
(use 'shannon.coding-primitives)
(def z (zipf 1000000))

(compress      0 z) ; ⇒ compresses to 1 byte
(compress 999999 z) ; ⇒ compresses to 4 bytes
```

As expected, smaller numbers require less space than larger numbers,
because the Zipf distribution assumes that smaller numbers are more
probable. Compare the above behavior with the result of using a
[uniform distribution](http://en.wikipedia.org/wiki/Uniform_distribution_%28discrete%29):

```clj
(def u (uniform 1000000))

(compress      0 u) ; ⇒ compresses to 3 bytes
(compress 999999 u) ; ⇒ compresses to 3 bytes
```

Coders are composeable. For example, to encode a sequence of up to 20
integers with the distribution previously defined by `z`, where the
number of integers is uniformly distributed:

```clj
(def count-coder (uniform (inc 20)))
(def arr-coder (variable-array z count-coder))

(def o (compress (range 13) arr-coder)) ; ⇒ compresses to 11 bytes
(decompress o arr-coder)                ; ⇒ (0 1 2 3 4 5 6 7 8 9 10 11 12)
```

Here's a coder for a tuple consisting of an english string, the
variable sequence of integers coded by `arr-coder` above, and a date:

```clj
(use 'shannon.base-coders)
(def tuple-coder (fixed-array [english-coder arr-coder date-coder]))
(def t ["Hello world!" (range 10) #inst "2014-01-01T00:00:00.000Z"])

(def o (compress t tuple-coder))
; ⇒ compresses to 24 bytes

(decompress o tuple-coder)
; ⇒ ("Hello world!" (0 1 2 3 4 5 6 7 8 9) #inst "2014-01-01T00:00:00.000-00:00")
```

Compare this with the default coder, which has to store type
information and uses integers with Long ranges:

```clj
(compress t) ; ⇒ compresses to 34 bytes
```

## To Do

Currently, this library is more useful as a set of cross-platform,
composeable encoding primitives rather than as a high-performance,
universal compression library. Practical compression and runtime
performance requires the addition of adaptive distribution
primitives with caching rather than using generic, static distributions
for each datatype. In addition, the following would be useful:

- Tagged language strings
- Tagged custom types
- Clojure/Clojurescript integration tests
- Improved documentation

## References

- [mathematicalmonk](https://www.youtube.com/user/mathematicalmonk)
  has an
  [information theory primer](https://www.youtube.com/watch?v=UrefKMSEuAI&list=PLE125425EC837021F),
  which includes a
  [detailed walk-through of writing a practical arithmetic coder](https://www.youtube.com/watch?v=ouYV3rBtrTI&list=PLE125425EC837021F&index=41)
- [A New Approximation Formula for Computing the N-th Harmonic Number](http://www.seriesmathstudy.com/sms/ApproximateFormulaHarmonicNum)
- [Coefficients for the Lanczos Approximation to the Gamma Function](http://mrob.com/pub/ries/lanczos-gamma.html)
- [Information Theory, Inference and Learning Algorithms](http://www.inference.phy.cam.ac.uk/mackay/itila/book.html) by David J. C. MacKay. Chapter 6 has an overview of stream coding.
- [Numerical Recipes: The Art of Scientific Computing](http://www.nr.com/)
- Probability Theory: The Logic of Science by E. T. Jaynes
- [MIT 6.050J: Information and Entropy](http://ocw.mit.edu/courses/electrical-engineering-and-computer-science/6-050j-information-and-entropy-spring-2008/index.htm)
- [MIT 6.450: Principles of Digital Communications I](http://ocw.mit.edu/courses/electrical-engineering-and-computer-science/6-450-principles-of-digital-communications-i-fall-2006/index.htm)

## License

Copyright © 2013–2014 Joshua B. Griffith.

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
