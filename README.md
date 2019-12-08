# kee-frame-sockets

[![Build Status](https://travis-ci.org/ingesolvoll/kee-frame-sockets.svg?branch=master)](https://travis-ci.org/ingesolvoll/kee-frame)

[![Clojars Project](https://img.shields.io/clojars/v/kee-frame-sockets.svg)](https://clojars.org/kee-frame)

Websocket support is activated by requiring the websocket namespace 
```clojure
(require '[kee-frame.websocket :as websocket])
```
Kee-frame hides the details of the websocket connection, leaving you with a couple of effects and events to control the
situation. First, but not necessarily first, you want to establish the connection. That is done through a custom effect 
in your event handler, like this:
```clojure
(reg-event-fx ::start-socket
               (fn [{:keys [db]} _]
                 {::websocket/open {:path         "/ws/"
                                    :dispatch     ::your-socket-receiver-event ;; The re-frame event receiving server messages.
                                    :format       :transit-json ;; Can be omitted, defaults to :edn
                                    :wrap-message (fn [message] (assoc message :authToken (-> db :user :auth-token)))}}))
```
`:dispatch` is the re-frame event that should receive server-sent messages.

`wrap-message` is a function used to transform the message just before sending to server. A typical use case is authentication
tokens or other identifiers.

This is how you send a message to the server:

```clojure
(reg-event-fx ::send-message
              (fn [{:keys [db]} _]
                {:dispatch [::websocket/send "/ws/" {:this-is "the message"
                                                     :will-be "Automatically translated to edn/json/transit/etc"}]}))
```
You do not have to think about establishing the websocket before sending messages to it. Messages will be queued and sent
when the socket becomes available.

You might want to track the status of your socket. There's a subscription for that, goes like this:

```clojure
 @(subscribe [:kee-frame.websocket/state "/ws/"])

;; {:output-chan #object[cljs.core.async.impl.channels.ManyToManyChannel], 
;; :state :connected, 
;; :ws-chan #object[chord.channels.t_chord$channels19899]}

```

Websockets in kee-frame should be considered experimental, but might very well work for you. Help or bug reports would be highly appreciated.
