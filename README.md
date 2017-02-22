# clj-struct

This is almost Python's struct module in Clojure. You can easily
converse between Clojure value and bytes stream which come from file,
network ...etc.

# How to use it 

[clj-struct "0.1.0"]

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

| Character | Endian                 |
|:---------:| ---------------------- |
| <         | little-endian          |
| >         | big-endian             |
| !         | network (= big-endian) |

Network-endian is available for those poor souls who claim they can’t
remember whether network byte order is big-endian or little-endian.

## Data type

After Order type is one or many Data Type. Available  Data types are:

| Character | Data type        | Size | Clojure Type |
|:---------:| ---------------- |:----:| ------------ | 
| c         | char             | 1    | Character    |
| b         | signed char      | 1    | Intger       |
| B         | unsigned char    | 1    | Integer      |
| ?         | boolean          | 1    | Boolean      |
| h         | short            | 2    | Integer      |
| H         | unsigned short   | 2    | Integer      |
| i         | integer          | 4    | Long         |
| I         | unsigned integer | 4    | Long         |
| l         | long             | 8    | BigInt       |
| L         | unsigned long    | 8    | BigInt       |
| f         | float            | 4    | Float        |
| d         | double           | 8    | Double       |

## Count of Data type

You can have a count before data type, for example "3i2H" is equal "iiiHH"

# License

Copyright © 2013 Mingqi Shao

Distributed under the Eclipse Public License, the same as Clojure.
