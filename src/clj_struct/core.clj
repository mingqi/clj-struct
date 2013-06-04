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

;; (defn long-to-byte-array [i]
;;   (-> 8 ByteBuffer/allocate (.putLong i) .array)
;;   )

;; (defn byte-array-to-long[ byte-array]
;;   (let [buff (ByteBuffer/allocate 8)]
;;     (dotimes [n (- 8 (count byte-array))]
;;       (if (> (first byte-array) 0)
;;         (.put buff (byte 0))
;;         (.put buff (byte -1))
;;         )
;;       )
;;     (doseq [n (range 0 (count byte-array))]
;;       (.put buff (nth byte-array n))
;;      )
;;     (.flip buff)
;;     (.getLong buff)
;;     ))


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
;;;;b: signed char, integer in clojure
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; (defmethod decode \b
;;   [c-type, bytes]
;;   {:pre [(= (count bytes) 1)]}
;;   (int (first bytes))
;;   )

;; (defmethod encode \b
;;   [c-type, value]
;;   {:pre [(>= (int value) -128) (<= (int value) 127)]}
;;   [(last (long-to-byte-array value))]
;;   )

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;B: unsigned char, integer in clojure
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; (defmethod decode \B
;;   [c-type, bytes]
;;   {:pre [(= (count bytes) 1)]}
;;   (int (number-of-unsigned-bytes (first bytes)))
;;   )

;; (defmethod encode \B
;;   [c-type value]
;;   {:pre [(>= value 0) (<= value 255)]}
;;   [(last (long-to-byte-array value))]
;;   )

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

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; h: short, size is 2
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; (defmethod decode \h
;;   [c-type, bytes]
;;   {:pre [(= 2 (count bytes))]}
;;   (int (number-of-signed-bytes bytes))
;;   )

;; (defmethod encode \h
;;   [c-type value]
;;   {:pre [(>= value -32768) (<= value 32767)]}
;;   (signed-bytes-of-number value 2)
;;   )


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; H: unsign short, size is 2
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; (defmethod decode \H
;;   [c-type, bytes]
;;   {:pre [(= 2 (count bytes))]}
;;   (int (number-of-unsigned-bytes bytes)))

;; (defmethod encode \H
;;   [c-type value]
;;   {:pre [(>= value 0) (<= value 65535)]}
;;   (unsigned-bytes-of-number value 2)
;;   )

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; i: short, size is 4
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; (defmethod decode \i
;;   [c-type, bytes]
;;   {:pre [(= 4 (count bytes))]}
;;   (int (number-of-signed-bytes bytes))
;;   )

;; (defmethod encode \i
;;   [c-type value]
;;   {:pre [(>= value -2147483648) (<= value 2147483647)]}
;;   (signed-bytes-of-number value 4)
;;   )


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; I: unsign int, size is 4
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; (defmethod decode \I
;;   [c-type, bytes]
;;   {:pre [(= 4 (count bytes))]}
;;   (long (number-of-unsigned-bytes bytes)))

;; (defmethod encode \I
;;   [c-type value]
;;   {:pre [(>= value 0) (<= value 4294967296)]}
;;   (unsigned-bytes-of-number value 4)
;;   )



(defn- size-of [c-type]
  )

(defn- is-digital [char]
  (and char (> (int char) 48 ) (< (int char) 57))
  )

(defn- is-format [c]
  (some #{c} FORMAT-CHARS)
 )

(defn parse-format [fmt]
  (cond
   (= 0 (count fmt)) []
   (-> fmt first is-format) (concat [(first fmt)] (-> fmt rest parse-format) )
   (and (-> fmt first is-digital) (-> fmt second is-format) ) (concat
                                                               (-> fmt first int (- 48) (repeat (second fmt)))
                                                               (->> fmt (drop 2) parse-format ))
   :else (throw (IllegalArgumentException. (str "there are illegal format: " fmt)))
   ))

(defn- seq-with-bytes [fmt, data]
  ;; (if-let [c-type (first (parse-format fmt)), n (length-of c-type)]
  ;;   (cons (apply vector (take n data))
  ;;         (lazy-seq  (drop n data)))
  ;;   nil
  ;;   )
  )

(defn pack [fmt & values]
  (let [ c-types (parse-format fmt) ]
    (if-not (= (count c-types) (count values))
      "exception: fmt and values it not match"
      (map decode c-types values)
      )))

(defn unpack[fmt, data]
  (let [c-types (parse-format fmt), bytes-seq (seq-with-bytes data)]
    (if-not (= (count c-types) (count bytes-seq))
      "exception: "
      (map encode c-types bytes-seq)
    )))

(defn calcsize [fmt]
  (reduce #(+ %1 %2) 0 (map #(size-of %) (parse-format fmt) ) )
  )

(defn -main [ & args]
  (println "this is main")
  )