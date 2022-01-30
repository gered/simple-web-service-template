(defproject net.gered/lein-template.simple-web-service "0.3.0"
  :description         "Simple Clojure web service project template."
  :url                 "https://github.com/gered/simple-web-service-template"
  :license             {:name "MIT License"
                        :url  "http://opensource.org/licenses/MIT"}

  :eval-in-leiningen   true

  :deploy-repositories [["releases" :clojars]
                        ["snapshots" :clojars]]

  :release-tasks       [["vcs" "assert-committed"]
                        ["change" "version" "leiningen.release/bump-version" "release"]
                        ["vcs" "commit"]
                        ["vcs" "tag" "v" "--no-sign"]
                        ["deploy"]
                        ["change" "version" "leiningen.release/bump-version"]
                        ["vcs" "commit" "bump to next snapshot version for future development"]
                        ["vcs" "push"]]

  )
