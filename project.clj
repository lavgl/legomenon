(defproject legomenon "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.10.3"]
                 [org.clojure/java.jdbc "0.7.12"]
                 [dk.simongray/datalinguist "0.1.163"]
                 [edu.stanford.nlp/stanford-corenlp "4.3.2" :classifier "models"]
                 [com.novemberain/pantomime "2.11.0"]
                 [org.xerial/sqlite-jdbc "3.36.0.3"]]
  :repl-options {:init-ns legomenon.core})
