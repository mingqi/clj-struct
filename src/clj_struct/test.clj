(ns clj-struct.test
  )

(defn ^java.lang.String ss [ ^java.lang.String s]
  (println "input is " s)
  s
  ;(.toUpperCase s)
  )

(defn f []
  (println "hello, mingqi")
  (definline m[] (println "xulei"))
  ;(m)
  )

(defn -main [ & args]
  (f)
;  (m)
  )
