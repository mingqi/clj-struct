# clj-struct

This is almost Python's struct module in Clojure. You can easily converse between Clojure value and bytes stream which come from file, network ...etc.



# How to use it 

```clojure
(ns your-package
  (:require [clj-struct.core :as clj-struct]))
```

## pack
use 'pack' function to encode clojure value to a sequence of bytes


```clojure
(clj-struct/pack "c?I" \a true 32767)
;;> the output will be a sequence of byte corresponding to format "c?I" and value
```

The first parameter is a format (format detail see below), and rest parameters are corresponding values. In above example, format "c?I" means three Clojure value: a character, a boolean and a unsigned integer, and rest of parameters are that three values.

## unpack
use "unpack" to decode a sequence of bytes to clojure values

```clojure
(clj-struct/unpack "c?I" your-bytes)
;;; here the your-bytes should be a sequence of bytes read from network,
;;; file or other source. Output will be three Clojure value: a chatacter value, a boolean value and a long value
```

# Format syntax

## Starting with Order Type

The first character of format can be use to specify the byte order, little-endian or big-endian. For example
```
"<f"
```
means Float type with little-endian order. The Byte Order is optional, default is big-endian.

There are three options:
* < : little-endian
* > : big-endian
* ! : network (= big-endian). This is available for those poor souls who claim they can’t remember whether network byte order is big-endian or little-endian.

## Data type

After Order type is one or many Data Type. Available  Data type are:
* c : character, byte size is 1, corresponding Clojure type is Character
* b : signed char, byte size is 1, corresponding Clojure type is Integer
* B : unsigned character, size is 1, corresponding Clojure type is Integer
* ? : boolean. size is 1, corresponding Clojure type is Boolean
* h : short. Size is 2, corresponding Clojure type is Integer
* H : unsigned short. Size is 2, corresponding Clojure type is Integer
* i : integer. Size is 4, corresponding Clojure type is Long
* I : unsigned integer. Size is 4, corresponding Clojure type is Long.
* l : long. Size is 8, corresponding Clojure type is BigInt
* L : unsigned long. Size is 8, corresponding Clojure type is BigInt
* f : float. Size is 4. Clojure type is Float.
* d : double. Size is 8. Clojure type is Double.

## Count of Data type

You can have a count before data type, for example "3i2H" is equal "iiiHH"