(defproject kee-frame-sockets "0.0.2"
  :description "Websocket support for kee-frame"
  :license {:name "Eclipse Public License"
            :url  "http://www.eclipse.org/legal/epl-v10.html"}
  :url "https://github.com/ingesolvoll/kee-frame-sockets"
  :min-lein-version "2.0.0"
  :dependencies [[org.clojure/clojure "1.10.0"]
                 [org.clojure/clojurescript "1.10.597"]
                 [re-frame "0.10.6"]
                 [org.clojure/core.async "0.3.442"]
                 [jarohen/chord "0.8.1"]]

  :deploy-repositories [["clojars" {:sign-releases false
                                    :url           "https://clojars.org/repo"
                                    :username      :env/clojars_username
                                    :password      :env/clojars_password}]])
