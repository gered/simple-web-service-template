# Leiningen Template: Simple Clojure Web Service

A Leiningen template intended for creating new Clojure web service projects utilizing [reitit](https://github.com/metosin/reitit).

This template primarily exists for my own personal use, so some stuff is definitely more oriented towards
my own particular preferences regarding setup and organization of a Clojure project.

## Usage

```text
$ lein new net.gered/simple-web-service [your-project-name-here]
```

The resulting project starts up via a `main` function and during startup expects to be able to read an EDN 
configuration file located in the current working directory called `config.edn`.

The project can be run simply by:

```text
$ lein run
```

A nREPL server will be started which can be connected to on port 7000 (configured via the aforementioned `config.edn`).

The web service's endpoints will be accessible over port 8080 (again, configured via the aforementioned `config.edn`).
The Swagger UI page will be available at `/api-docs/` e.g. http://localhost:8080/api-docs/

## Other Templates!

I have a few other "simple" Leiningen project templates that are along the same vein as this one:

* [Simple Clojure App](https://github.com/gered/simple-app-template) - Very simple starter base for non-web projects.
* [Simple Clojure Web Site](https://github.com/gered/simple-web-site-template) - For web sites using only server-side rendered HTML (**no** ClojureScript!).
* [Simple Clojure / ClojureScript Web App](https://github.com/gered/simple-web-app-template) - For web apps using Clojure and ClojureScript.

## License

Copyright © 2021 Gered King

Distributed under the the MIT License. See LICENSE for more details.
