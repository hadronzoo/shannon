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

Non-atomic coders can be composed. For example, to encode a sequence
of up to 20 integers with the distribution previously defined by `z`,
where the number of integers is uniformly distributed:

```clj
(def count-coder (uniform (inc 20)))
(def arr-coder (variable-array z count-coder))

(def o (compress (range 13) arr-coder)) ; ⇒ compresses to 11 bytes
(decompress o arr-coder)                ; ⇒ (0 1 2 3 4 5 6 7 8 9 10 11 12)
```

Compare this with the default coder, which has to store type
information and uses integers with Long ranges:

```clj
(compress (range 13)) ; ⇒ compresses to 22 bytes
```

## To Do

- Integration tests
- Tagged language strings
- Tagged custom types
- Documentation

## License

Copyright © 2013–2014 Joshua B. Griffith.

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
