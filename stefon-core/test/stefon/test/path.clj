(ns stefon.test.path
  (:require [stefon.path :refer :all]
            [stefon.util :refer (dump)]
            [stefon.digest :as digest]
            [stefon.settings :as settings]
            [clojure.java.io :as io]
            [clojure.test :refer :all]))

(def sha1 "01234567890123456789012345678901")
(def asset-uris ["/assets/something.js" "/assets/something"])
(def non-asset-uris ["asdasd/assets/" "s/assets/" "/asseTs/" "/assets"])
(def digested-paths [["/assets/js/file-01234567890123456789012345678901.js"
                      ["/assets/js/file" "js"]]
                     ["file-01234567890123456789012345678901.js"
                      ["file" "js"]]
                     ["/assets/js/file-01234567890123456789012345678901.js.coffee"
                      ["/assets/js/file" "js.coffee"]]])
(def undigested-paths ["/assets/js/file-4567890123456789012345678901.js"])

(deftest asset-uri-works
  (doseq [uri asset-uris] (is (asset-uri? uri)))
  (doseq [uri non-asset-uris] (is (not (asset-uri? uri)))))

(deftest split-digested-path-works
  (doseq [[path expected] digested-paths]
    (let [[_ pathpart digest extension] (split-digested-path path)]
      (is (and (= digest sha1)
               (= [pathpart extension] expected)))))

  (doseq [path undigested-paths]
    (is (nil? (split-digested-path path)))))

(deftest split-path-works
  (doseq [[path expected] digested-paths]
    (let [[_ pathpart digest extension] (split-digested-path path)
          [_ pathpart2 extension2] (split-path path)]
      (is (= pathpart2 (str pathpart "-" digest))
          (= extension2 extension)))))

(deftest is-digest-works
  (doseq [[path expected] digested-paths]
    (is (digest-path? path))))

(deftest path->undigested-works
  (doseq [[path [pathpart extension]] digested-paths]
    (is (= (str pathpart "." extension) (path->undigested path)))))

(deftest path->digested-works
  (with-redefs [digest/digest (constantly sha1)]
    (is (= (path->digested "asd.js" "") "asd-01234567890123456789012345678901.js"))
    (is (= (path->digested "asd.js.coffee" "") "asd-01234567890123456789012345678901.js.coffee"))))

(deftest uri->adrf-works
  (is (= (uri->adrf "/assets/some-filename") "some-filename")))

(deftest adrf->filename-works
  (is (= (adrf->filename "asset-root" "some-filename") "asset-root/some-filename")))


(deftest ->normalized-works-for-simple-paths
  (is (= (->normalized "") ""))
  (is (= (->normalized ".") "."))
  (is (= (->normalized "/") "/"))
  (is (= (->normalized "a/b/") "a/b"))
  (is (= (->normalized "a//b//c/d") "a/b/c/d")))

(deftest ->normalized-removes-current-dir
  (is (= (->normalized "./") "."))
  (is (= (->normalized "./path/to/file") "path/to/file"))
  (is (= (->normalized "././path/to/file") "path/to/file"))
  (is (= (->normalized "path/./to/file") "path/to/file"))
  ; "../" at the beginning of a path should be left alone
  (is (= (->normalized "../path") "../path")))

(deftest ->normalized-removes-parent-dirs
  (is (= (->normalized "a/../b") "b"))
  (is (= (->normalized "/../b") "/b"))
  (is (= (->normalized "a/../../b") "../b"))
  (is (= (->normalized "a/b/../../c/d") "c/d"))
  (is (= (->normalized "../") "..")))

(deftest relative-to-works
  (is (= (relative-to "base/path" "base/path/file") "file"))
  (is (= (relative-to "base/path" "base/path/directory/file") "directory/file"))
  (is (= (relative-to "/base/path" "/base/path/file") "file"))
  (is (= (relative-to "/base/path" "/base/path/directory/file") "directory/file"))
  (is (= (relative-to "unrelated" "base/path") "../base/path"))
  (is (= (relative-to "/unrelated" "/base/path") "../base/path"))
  (is (thrown? AssertionError (relative-to "/absolute/path" "relative/path")))
  (is (thrown? AssertionError (relative-to "relative/path" "/absolute/path"))))
