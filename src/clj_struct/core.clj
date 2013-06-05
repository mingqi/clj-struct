(ns clj-struct.core
  (:import (java.nio ByteBuffer))
  )


(def number-format-config
  { \b , {:signed true
          :size 1
          }
    \B , {:signed false
          :size 1
          }
    \h , {:signed true
          :size 2
          }
    \H , {:signed false
          :size 2
          }
    \i , {:signed true
          :size 4
          }
    \I , {:signed false
          :size 4
          }
    \l , {:signed true
          :size 4
          }
    \L , {:signed false
          :size 4
          }
    \q , {:signed true
          :size 8
          }
    \Q , {:signed false
          :size 8
          }
   }
  )


(defn number-of-signed-bytes [ & bytes]
  (BigInteger. (byte-array (flatten bytes)))
  )

(defn number-of-unsigned-bytes [ & bytes]
  (BigInteger. (byte-array (cons (byte 0) (flatten bytes))))
  )

(defn unsigned-bytes-of-number [ number, byte-size]
  (let [byte-array (-> (str number) (BigInteger. ) .toByteArray)]
    (apply vector (concat
                   (repeat (- byte-size (count byte-array)) (byte 0))
                   (take-last (min byte-size (count byte-array)) byte-array)))
           ))

(defn signed-bytes-of-number [ number, byte-size ]
  (let [byte-array (-> (str number) (BigInteger. ) .toByteArray)]
    (let [pad-byte (if (< number 0 ) (byte -1) (byte 0))]
      (apply vector (concat
                     (repeat (- byte-size (count byte-array)) pad-byte)
                     (take-last (min byte-size (count byte-array)) byte-array)))
      )))

(defn size-of [format]
  (condp = format
    \c 1
    \b 1
    \B 1
    \? 1
    \h 2
    \H 2
    \i 4
    \I 4
    \l 4
    \L 4
    \q 8
    \Q 8
    \f 4
    \d 8
    )
  )

(def FORMAT-CHARS [\c \b \B \? \h \H \i \I \l \L \q \Q \f \d])

(defmulti decode (fn [c-type bytes] c-type))
(defmulti encode (fn [c-type value] c-type))

(defmethod decode :default
  [c-type bytes]
  (let [c-type-config (get number-format-config c-type)]
    (assert (= (count bytes) (:size c-type-config)))
    (if (:signed c-type-config)
      (number-of-signed-bytes bytes)
      (number-of-unsigned-bytes bytes)
      )))

(defmethod encode :default
  [c-type, value]
  (let [c-type-config (get number-format-config c-type)]
    (if (:signed c-type-config)
      (do
        (assert (<= value (dec (Math/pow 2 (dec (* 8 (:size c-type-config)))))))
        (assert (>= value (- (Math/pow 2 (dec (* 8 (:size c-type-config)))))))
        (signed-bytes-of-number value (:size c-type-config)))
      (do
        (assert (<= value (dec (Math/pow 2 (* 8 (:size c-type-config))))))
        (assert (>= value 0))
        (unsigned-bytes-of-number value (:size c-type-config)))
      )))

;;; c: single character,  data type is character in clojure
(defmethod decode \c
  [c-type, bytes]
  {:pre [(= (count bytes) 1) (> (first bytes) 0) (< (first bytes) 128) ]}
  (char (first bytes))
  )

(defmethod encode \c
  [c-type, ^java.lang.Character value]
  {:pre [(> (int value) 0) (<= (int value) 127)]}
  [(byte (char value))]
  )


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; ?: boolean, size is 1, Boolean in Clojure
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defmethod decode \?
  [c-type, bytes]
  {:pre [(= (count bytes) 1)]}
  (if (= 0 (first bytes))
    false
    true
    ))

(defmethod encode \?
  [c-type value]
  (if value
    [(byte 1)]
    [(byte 0)]
    ))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; f: float, size is 4
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defmethod decode \f
  [c-type, bytes]
  (let [buffer (ByteBuffer/allocate 4)]
    (.put buffer (byte-array 4 bytes))
    (.flip buffer )
    (.getFloat buffer )
    ))
(defmethod encode \f
  [c-type, value]
  (-> 4 ByteBuffer/allocate (.putFloat (float value)) .array)
  )

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; d: double , size is 8
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defmethod decode \d
  [c-type, bytes]
  (let [buffer (ByteBuffer/allocate 8)]
    (.put buffer (byte-array 8 bytes))
    (.flip buffer )
    (.getDouble buffer )
    ))
(defmethod encode \d
  [c-type, value]
  (-> 8 ByteBuffer/allocate (.putDouble (double value)) .array)
  )



(defn- is-digital [char]
  (and char (> (int char) 48 ) (< (int char) 57))
  )

(defn- is-format [c]
  (some #{c} FORMAT-CHARS)
 )

(defn parse-order-type [fmt]
  (if-let [order-c (some #{(first fmt)}  [\> \< \!])]
    (condp = order-c
      \> :big-endian
      \< :little-endian
      \! :big-endian)
    :big-endian))

(defn parse-ctype-seq [fmt]
  (let [fmt (if (some #{(first fmt)} [\< \> \!])  (rest fmt) fmt)]
    (cond
     (= 0 (count fmt)) []
     (-> fmt first is-format) (concat [(first fmt)] (-> fmt rest parse-ctype-seq) )
     (and (-> fmt first is-digital) (-> fmt second is-format) )
     (concat (-> fmt first int (- 48) (repeat (second fmt)))
             (parse-ctype-seq (drop 2 fmt)))
     :else (throw (IllegalArgumentException. (str "there are illegal format: " fmt)))
     )))

(defn- group-byte-with-format [c-types, bytes]
  (if-let [c-type (first c-types)]
    (let [size (size-of c-type)]
      (cons (take size bytes) (group-byte-with-format (rest c-types) (drop size bytes)))
      )))

(defn calcsize [fmt]
  (reduce #(+ %1 %2) 0 (map #(size-of %) (parse-ctype-seq fmt) ) )
  )

(defn pack [fmt & values]
  (let [ c-types (parse-ctype-seq fmt), order-type (parse-order-type fmt) ]
    (if-not (= (count c-types) (count values))
      (throw (IllegalArgumentException. "byte size is not correct"))
      (vec (flatten (map #(if (= :little-endian (parse-order-type fmt))
                            (reverse (encode %1 %2))
                            (encode %1 %2)
                            )
                     c-types values)))
      )))

(defn unpack[fmt, bytes]
  (if-not (= (calcsize fmt) (count bytes))
    (throw (IllegalArgumentException. "byte's length is not match format")))

  (let [c-types (parse-ctype-seq fmt), byte-groups (group-byte-with-format c-types bytes), order-type (parse-order-type fmt)]
    (map #(if (= :little-endian order-type)
            (decode %1 (reverse %2))
            (decode %1 %2))
         c-types byte-groups)
    ))


(defn -main [ & args]
  (println "this is main")
  )