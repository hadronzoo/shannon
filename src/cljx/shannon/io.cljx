(ns shannon.io
  #+clj
  (import [com.github.jinahya.io.bit BitInput BitOutput StreamInput StreamOutput
           BufferInput BufferOutput ChannelInput ChannelOutput]
          [java.io Closeable InputStream OutputStream ByteArrayInputStream
           ByteArrayOutputStream EOFException]
          [java.nio ByteBuffer]
          [java.nio.channels Channel ReadableByteChannel WritableByteChannel]))

(defprotocol EmitsBits
  (read! [o])
  (source [o]))

(defprotocol ConsumesBits
  (write! [o bit])
  (target [o]))

(defprotocol BitCoder
  (with-bits! [c bits])
  (finalize! [c]))

(defprotocol AsBits
  (bit-source [o])
  (bit-sink [o]))

#+clj
(deftype JavaBitSource [^BitInput input]
  EmitsBits
  (read! [_]
    (try
      (.readBoolean input)
      (catch EOFException e
        nil)))
  (source [_] (.getSource (.getInput input)))

  Closeable
  (close [o] (.close input)))

#+clj
(deftype JavaBitSink [^BitOutput output]
  ConsumesBits
  (write! [o bit] (.writeBoolean output bit))
  (target [o] (.getTarget (.getOutput output)))

  BitCoder
  (finalize! [c] (.align output 1))

  Closeable
  (close [o] (.close output)))

#+cljs
(deftype JSBitArrayBitSource [^array input]
  EmitsBits
  (read! [_] (.shift input))
  (source [_] input))

#+cljs
(deftype JSBitArrayBitSink [^array output]
  ConsumesBits
  (write! [_ bit] (.push output bit))
  (target [_] output)

  BitCoder
  (finalize! [_] output))

#+cljs
(deftype JSByteArrayBitSource [^array input ^:mutable current-byte ^:mutable pos]
  EmitsBits
  (read! [o]
    (when current-byte
      (let [bit (bit-test current-byte pos)]
        (if (zero? pos)
          (do
            (set! (.-current-byte o) (.shift input))
            (set! (.-pos o) 7))
          (set! (.-pos o) (dec pos)))
        bit)))
  (source [_] input))

#+cljs
(defn bytearray-source [arr]
  (JSByteArrayBitSource. arr (.shift arr) 7))

#+cljs
(deftype JSByteArrayBitSink [^array output ^:mutable current-byte ^:mutable index]
  ConsumesBits
  (write! [o bit]
    (let [b (if bit 1 0)]
      (set! (.-current-byte o) (bit-or current-byte
                                       (bit-shift-left b index))))
    (if (zero? index)
      (do
        (.push output current-byte)
        (set! (.-index o) 7)
        (set! (.-current-byte o) 0))
      (set! (.-index o) (dec index)))
    output)
  (target [_] output)

  BitCoder
  (finalize! [o]
    (when (< index 7)
      (.push output current-byte)
      (set! (.-current-byte o) 0)
      (set! (.-index o) 7))
    output))

#+cljs
(defn bytearray-sink [arr]
  (JSByteArrayBitSink. arr 0 7))

#+clj
(extend-protocol AsBits
  InputStream
  (bit-source [o] (JavaBitSource. (BitInput. (StreamInput. o))))

  OutputStream
  (bit-sink [o] (JavaBitSink. (BitOutput. (StreamOutput. o))))

  ByteBuffer
  (bit-source [o] (JavaBitSource. (BitInput. (BufferInput. o))))
  (bit-sink [o] (JavaBitSink. (BitOutput. (BufferOutput. o))))

  ReadableByteChannel
  (bit-source [o] (JavaBitSource. (BitInput. (ChannelInput. o nil))))

  WritableByteChannel
  (bit-sink [o] (JavaBitSink. (BitOutput. (ChannelOutput. o nil))))

  #=(java.lang.Class/forName "[B") ;; byte array
  (bit-source [o] (bit-source (ByteArrayInputStream. o))))

#+cljs
(extend-protocol AsBits
  array
  (bit-source [o] (bytearray-source o))
  (bit-sink [o] (bytearray-sink o)))

(defn default-bit-sink []
  #+clj (bit-sink (ByteArrayOutputStream.))
  #+cljs (bit-sink (array)))
