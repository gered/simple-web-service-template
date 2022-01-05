{{=<% %>=}}
(ns <%root-ns%>.core
  (:gen-class)
  (:require
    [clojure.tools.logging :as log]
    [cprop.core :refer [load-config]]
    [hiccup.page :refer [html5]]
    [org.httpkit.server :as http-kit]
    [mount.core :as mount :refer [defstate]]
    [muuntaja.core :as m]
    [nrepl.server :as nrepl]
    [reitit.coercion.schema]
    [reitit.ring :as ring]
    [reitit.ring.coercion :as coercion]
    [reitit.ring.middleware.exception :as exception]
    [reitit.ring.middleware.multipart :as multipart]
    [reitit.ring.middleware.muuntaja :as muuntaja]
    [reitit.ring.middleware.parameters :as parameters]
    [reitit.swagger :as swagger]
    [reitit.swagger-ui :as swagger-ui]
    [ring.middleware.reload :refer [wrap-reload]]
    [ring.util.http-response :refer :all]
    [schema.core :as s]))

(declare config)

;;
;; TODO: other app stuff goes here ...
;;

; example exception handler that logs all unhandled exceptions thrown by your routes
(def exception-middleware
  (exception/create-exception-middleware
    (merge
      exception/default-handlers
      {::exception/default
       (fn [e {:keys [uri request-method remote-addr] :as request}]
         (log/error e (format "Unhandled exception during request - %s %s from %s"
                              request-method uri remote-addr))
         (exception/default-handler e request))})))

; example middleware to enforce request authorization via simple api token header
(defn wrap-auth-restrict
  [handler]
  (fn [{:keys [headers] :as request}]
    (let [api-key (get headers "x-api-key")]
      (if (= "secret" api-key)
        (handler request)
        (unauthorized "unauthorized!")))))

(defstate handler
  :start
  (ring/ring-handler
    (ring/router
      [["/status"
        {:swagger {:tags ["Infrastructure"]}
         :get     {:summary "Tests and returns the current status of this web service."
                   :handler (fn [_]
                              ; TODO: add your own real test and more detailed status response
                              (ok "up!"))}}]

       ["/api"
        ; TODO: replace these with your own endpoints ...

        ["/foo"
         {:swagger    {:security [{:my-auth []}]}           ; tell swagger this route is restricted
          :middleware [wrap-auth-restrict]                  ; apply auth middleware to this route
          :get        {:summary   "Gets a foo"
                       :responses {200 {:body {:foo s/Str}}}
                       :handler   (fn [_]
                                    (let [foo {:foo (str "This is a foo that was generated on: " (java.util.Date.))}]
                                      (log/info "Returning a foo:" foo)
                                      (ok foo)))}
          :post       {:summary    "Posts a foo"
                       :parameters {:body {:foo s/Str}}
                       :responses  {200 {:body s/Str}}
                       :handler    (fn [{{foo :body} :parameters}]
                                     (log/info "Posted a foo: " foo)
                                     (ok "Thanks for the foo!"))}}]

        ["/math"
         {:get {:summary    "Perform a simple math calculation"
                :parameters {:query {:a  s/Num
                                     :b  s/Num
                                     :op s/Str}}
                :responses  {200 {:body {:result s/Num}}
                             400 {:body s/Str}}
                :handler    (fn [{{{:keys [a b op]} :query} :parameters}]
                              (log/info "Performing math calculation: " a op b)
                              (ok
                                {:result
                                 (case op
                                   "+" (+ a b)
                                   "-" (- a b)
                                   "*" (* a b)
                                   "/" (/ a b)
                                   (bad-request! {:what "Invalid operation"}))}))}}]

        ; ---
        ]

       ["" {:no-doc  true
            :swagger {:securityDefinitions
                      ; tell swagger the details of our auth method(s)
                      {:my-auth {:type "apiKey"
                                 :in   "header"
                                 :name "X-API-Key"}}}}

        ; default root handler
        ["/"
         {:get {:handler (fn [_]
                           (ok (html5 [:h2 "<%name%>"])))}}]

        ["/swagger.json"
         {:get {:swagger {:info {:title "<%name%> API"}}
                :handler (swagger/create-swagger-handler)}}]
        ["/api-docs*"
         (swagger-ui/create-swagger-ui-handler
           {:config {}})]]

       ]

      {:data {:coercion   reitit.coercion.schema/coercion
              :muuntaja   m/instance
              :middleware [parameters/parameters-middleware ; query-params & form-params
                           muuntaja/format-negotiate-middleware ; content-negotiation
                           muuntaja/format-response-middleware ; encoding response body
                           exception-middleware   ; exception handling
                           muuntaja/format-request-middleware ; decoding request body
                           coercion/coerce-response-middleware ; coercing response body
                           coercion/coerce-request-middleware ; coercing request parameters
                           multipart/multipart-middleware   ; multipart
                           ]}})
    (ring/routes
      (ring/create-default-handler))))

(defn wrap-base
  [handler]
  (as-> handler h
        (if (:dev? config) (wrap-reload h) h)
        ; TODO: other base middleware here
        ))

;;

(defstate ^{:on-reload :noop} config
  :start
  (do
    (log/info "Loading config.edn")
    (load-config :file "config.edn")))

(defstate ^{:on-reload :noop} repl-server
  :start
  (let [{:keys [port bind]
         :or   {port 7000
                bind "127.0.0.1"}} (:nrepl config)
        server (nrepl/start-server :port port :bind bind)]
    (log/info (format "Starting nREPL server listening on %s:%d" bind port))
    server)
  :stop
  (when repl-server
    (log/info "Stopping nREPL server")
    (nrepl/stop-server repl-server)))

(defstate ^{:on-reload :noop} http-server
  :start
  (let [{:keys [port bind]
         :or   {port 8080
                bind "0.0.0.0"}} (:http-server config)
        server (http-kit/run-server
                 (wrap-base #'handler)
                 {:port                 port
                  :ip                   bind
                  :server-header        nil
                  :legacy-return-value? false})]
    (log/info (format "Started HTTP server listening on %s:%d" bind port))
    server)
  :stop
  (when http-server
    (log/info "Stopping HTTP server")
    (http-kit/server-stop! http-server)
    nil))

;;

(defn -main
  [& args]
  (log/info "<%name%> is starting up ...")
  (mount/start-with-args args)
  (log/info "Ready!"))
