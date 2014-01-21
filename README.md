# Shannon

An extensible arithmetic coding toolkit for Clojure and Clojurescript.

## Artifacts

`shannon` artifacts are
[released to clojars](https://clojars.org/com.joshuagriffith/shannon).

If you are using Maven, add the following repository definition to
your `pom.xml`:

```xml
<repository>
  <id>clojars.org</id>
  <url>http://clojars.org/repo</url>
</repository>
```

Or with Leiningen:

```clj
[com.joshuagriffith/shannon "0.1.0"]
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
[uniform distribution](http://en.wikipedia.org/wiki/Uniform_distribution_(discrete)):

```clj
(def u (uniform 1000000))

(compress      0 u) ; ⇒ compresses to 3 bytes
(compress 999999 u) ; ⇒ compresses to 3 bytes
```

## License

Copyright © 2013--2014 Joshua B. Griffith.

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
