(ns clj-struct.test.core
  (:use clojure.test)
  (:require [clj-struct.core :as core]
            [clojure.string :as str])
  )

(defn byte-to-bits[ b ]
  (str/join ""
            (for [x (range 7 -1 -1)]
              (str (bit-and (bit-shift-right b x) 1)))))
(defn byte-to-bits-vector [ b-vector ]
  (apply vector (map #(byte-to-bits %) b-vector))
  )

(defn bits-to-byte [ bits ]
  ;; (last (core/long-to-byte-array ;;; value will be cast to long after bitwise. That's why we need this step
  (last (core/signed-bytes-of-number 
         (reduce (fn[_byte, bit]
                   (let [ shifted-byte (bit-shift-left _byte 1)]
                     (if (= \1 bit)
                       (bit-set shifted-byte 0)
                       shifted-byte
                       )))
                 (byte 0) bits)
         1))
         ;; ))
)

(defn bits-to-byte-vector [ bits-array ]
  (apply vector (map #(bits-to-byte %) bits-array))
  )


;;;;;;;;;;;;;; start testing ;;;;;;;;;;;;;;;;;;;;;;
(deftest test-format-c
  (testing "decoding"
    (is (= \a (core/decode \c [(bits-to-byte "01100001")])) "normal case")
    (is (thrown? Throwable (core/decode \c [(bits-to-byte "10000000")])) "byte value great than 127"))
  (testing "encoding"
    (let [bytes (core/encode \c \a)]
      (is (= 1 (count bytes)) "the size of 'c' should be 1")
      (is (= "01100001" (byte-to-bits (first bytes))) "encoding normal")
      )
    (is (thrown? AssertionError (core/encode \c 256)) "only range (0 - 127) value is accepable")
    ))

(deftest test-format-b
  (testing "decoding"
    (is (= -128 (core/decode \b [(bits-to-byte "10000000")])) "can accept signed byte")
    (is (thrown? AssertionError (core/decode \b [(bits-to-byte "10000000") (bits-to-byte "10000000")])) "acceptable byte size is 1"))
  (testing "encoding"
    (is (= "01111110" (byte-to-bits (first (core/encode \b (byte 126))))))
    (is (= "10000000" (byte-to-bits (first (core/encode \b (byte -128))))))
    (is (= "10000000" (byte-to-bits (first (core/encode \b  -128)))) "Integer also can be input")
    (is (thrown? AssertionError (first (core/encode \b -129))) "value's range should be (-128 127)")
    (is (thrown? AssertionError (first (core/encode \b  128))) "value's range should be (-128 127)")
    )
  )

(deftest test-format-B
  (testing "decoding"
    (is (= 126 (core/decode \B [(bits-to-byte "01111110")])))
    (is (= 254 (core/decode \B [(bits-to-byte "11111110")])))
    )

  (testing "encoding"
    (is (= "11111111" (byte-to-bits (first (core/encode \B 255)))) "first position (sign postion) is 1")
    (is (= "01111110" (byte-to-bits (first (core/encode \B 126)))) "first position (sign postion) is 0")
    (is (thrown? AssertionError (first (core/encode \B 256))))
    (is (thrown? AssertionError (first (core/encode \B -1))))
    )
  )


(deftest test-format-?
  (testing "decoding"
    (is (core/decode \? [(bits-to-byte "00000001")]))
    (is (not (core/decode \? [(bits-to-byte "00000000")])))
    (is (core/decode \? [(bits-to-byte "10000001")]))
    )

  (testing "encoding"
    (is (= "00000000" (byte-to-bits (first (core/encode \? false)))))
    (is (= "00000000" (byte-to-bits (first (core/encode \? nil)))))
    (is (= "00000001" (byte-to-bits (first (core/encode \? true)))))
    (is (= "00000001" (byte-to-bits (first (core/encode \? 1)))))
    )
  )

(deftest test-format-h
  (testing "decoding"
    (is (= 257 (core/decode \h (bits-to-byte-vector ["00000001", "00000001"]))))
    (is (= -32767 (core/decode \h (bits-to-byte-vector ["10000000", "00000001"]))))
    )

  (testing "encoding"
    (is (= ["00000001", "00000001"] (byte-to-bits-vector (core/encode \h 257))))
    (is (= ["10000000", "00000001"] (byte-to-bits-vector (core/encode \h -32767))))
    )
  )

(deftest test-format-H 
  (testing "decoding"
    (is (= 257 (core/decode \H (bits-to-byte-vector ["00000001", "00000001"]))))
    (is (= 32769 (core/decode \H (bits-to-byte-vector ["10000000", "00000001"]))))
    )

  (testing "encoding"
    (is (= ["00000001", "00000001"] (byte-to-bits-vector (core/encode \H 257))))
    (is (= ["10000000", "00000001"] (byte-to-bits-vector (core/encode \H 32769))))
    )
  )

(deftest test-format-i
  (testing "decoding"
    (is (= 257 (core/decode \i (bits-to-byte-vector ["00000000" "00000000" "00000001", "00000001"]))))
    (is (= -32767 (core/decode \i (bits-to-byte-vector ["11111111", "11111111", "10000000", "00000001"]))))
    )

  (testing "encoding"
    (is (= ["00000000" "00000000" "00000001", "00000001"] (byte-to-bits-vector (core/encode \i 257))))
    (is (= ["11111111", "11111111","10000000", "00000001"] (byte-to-bits-vector (core/encode \i -32767))))
    )
  )

(deftest test-format-I 
  (testing "decoding"
    (is (= 257 (core/decode \I (bits-to-byte-vector ["00000000", "00000000", "00000001", "00000001"]))))
    (is (= 2147516417 (core/decode \I (bits-to-byte-vector ["10000000", "00000000", "10000000", "00000001"]))))
    )

  (testing "encoding"
    (is (= ["00000000", "00000000", "00000001", "00000001"] (byte-to-bits-vector (core/encode \I 257))))
    (is (= ["10000000","00000000", "10000000", "00000001"] (byte-to-bits-vector (core/encode \I 2147516417))))
    ))

(deftest test-format-f
  (testing "decoding"
    (is (= (float 98.6) (core/decode \f (bits-to-byte-vector ["01000010", "11000101", "00110011", "00110011"]) )))
    )

  (testing "encoding"
    (is (= ["01000010", "11000101", "00110011", "00110011"] (byte-to-bits-vector (core/encode \f (float 98.6))))))
  )

(deftest test-format-d
  (testing "decoding"
    (is (= (double 98.6) (core/decode \d (bits-to-byte-vector ["01000000", "01011000", "10100110", "01100110", "01100110", "01100110", "01100110", "01100110"]) )))
    )

  (testing "encoding"
    (is (= ["01000000", "01011000", "10100110", "01100110", "01100110", "01100110", "01100110", "01100110"] (byte-to-bits-vector (core/encode \d (double 98.6))))))
  )

(deftest parse-ctype-seq
  (testing "simplest case: not number in format"
      (let [c-types (core/parse-ctype-seq "cl")]
        (is (=  (count c-types) 2))
        (is (= (first c-types) \c)  )
        (is (= (second c-types) \l ) )
        ))

  (testing "there are number format"
    (let [c-types (core/parse-ctype-seq "2c") ]
      (is (= (count c-types) 2))
      (is (= (first c-types) \c)  )
      (is (= (second c-types) \c ) )
      ))

  (testing "conbine number and normal format"
    (let [c-types (core/parse-ctype-seq"l3cd")]
      (is (= (count c-types) 5))
      (is (= (first c-types) \l))
      (is (= (second c-types) \c))
      (is (= (nth c-types 4) \d))
      ))

  (testing "illegal format"
    (is (thrown? IllegalArgumentException (core/parse-ctype-seq "lcXd")))
    (is (thrown? IllegalArgumentException (core/parse-ctype-seq "lcd2")))
    ))


(deftest calcsize
  (is  (= (core/calcsize "2b3i") 14))
  )

(deftest pack
  (is (= ["01100001", "11111111", "10000000", "00000001", "11111111", "11111111", "10000000", "00000001"] (byte-to-bits-vector (core/pack "cbB?i" \a -1 128 true  -32767))))
  (is (= ["01100001", "11111111", "10000000", "00000001", "00000001", "10000000", "11111111", "11111111",  ] (byte-to-bits-vector (core/pack "<cbB?i" \a -1 128 true  -32767))))
  (is (thrown? IllegalArgumentException (core/pack "cbB?ii" \a -1 128 true  -32767)))
  )

(deftest unpack
  (is (= [\a -1 128 true -32767] (core/unpack "cbB?i" (bits-to-byte-vector ["01100001", "11111111", "10000000", "00000001", "11111111", "11111111", "10000000", "00000001"]))))
  (is (= [\a -1 128 true -32767] (core/unpack "<cbB?i" (bits-to-byte-vector ["01100001", "11111111", "10000000", "00000001", "00000001", "10000000", "11111111", "11111111" ]))))
  )