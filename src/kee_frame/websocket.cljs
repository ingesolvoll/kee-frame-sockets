(ns ^:no-doc kee-frame.websocket
  (:require
    [clojure.core.async :refer [<! >! chan close!]]
    [clojure.string :as str]
    [chord.client :as chord]
    [re-frame.core :as rf])
  (:require-macros
   [cljs.core.async.macros :refer [go go-loop]]))

(defn websocket-url [path]
  (if (str/starts-with? path "/")
    (str (if (= "https:" (-> js/document .-location .-protocol))
           "wss://"
           "ws://")
         (-> js/document .-location .-host)
         path)
    ;; Consider this an url for now.
    path))

(defn- receive-messages! [ws-chan {:keys [path dispatch]
                                   :as   socket-config}]
  (go-loop []
    (if-let [message (<! ws-chan)]
      (do (when false (rf/dispatch [::log path :received (js/Date.) message]))
          (rf/dispatch [dispatch message])
          (recur))
      (rf/dispatch [::disconnected socket-config]))))

(defn- send-messages!
  [path buffer-chan ws-chan wrap-message]
  (let [wrap-message (or wrap-message identity)]
    (go-loop []
      (when-let [message (<! buffer-chan)]                  ;; buffer-chan may be closed
        (if (>! ws-chan (wrap-message message))             ;; if ws-chan is closed put message back in buffer-chan
          (do (when false (rf/dispatch [::log path :sent (js/Date.) message]))
              (recur))
          (>! buffer-chan message))))))

(defn- socket-for [db path]
  (or
    (get-in db [::sockets path])
    {:buffer-chan (chan)
     :state       :initializing}))

(rf/reg-event-db
  ::log
  (fn [db [_ path type time message]]
    (update-in db [::sockets path :log] conj {:time    time
                                              :message message
                                              :type    type})))

(rf/reg-event-db
  ::error
  (fn [db [_ path message]]
    (update-in db [::sockets path] merge {:state   :error
                                          :message message})))

(rf/reg-event-fx
  ::disconnected
  (fn [{:keys [db]} [_ {:keys [path reconnect?]
                      :as   socket-config}]]
    (when (get-in db [::sockets path])
      (merge
        {:db (update-in db [::sockets path] merge {:state :disconnected})}
        (when reconnect?
          {::open socket-config})))))

(rf/reg-event-fx
  ::connected
  (fn [{:keys [db]} [_ ws-chan {:keys [path wrap-message]
                              :as   socket-config}]]
    (let [{:keys [buffer-chan]} (socket-for db path)]
      (send-messages! path buffer-chan ws-chan wrap-message)
      (receive-messages! ws-chan socket-config)
      {:db (update-in db [::sockets path] merge {:ws-channel  ws-chan
                                                 :buffer-chan buffer-chan
                                                 :state       :connected})})))

(rf/reg-fx ::open
           (fn [_ {:keys [path format]
                   :as   socket-config}]
             (go
              (let [url (websocket-url path)
                    {:keys [ws-channel error]} (<! (chord/ws-ch url {:format format}))]
                (if error
                  (rf/dispatch [::error path error])
                  (rf/dispatch [::connected ws-channel socket-config]))))))

(rf/reg-event-fx ::close (fn [{:keys [db]} [_ path]]
                          (let [{:keys [buffer-chan ws-channel]} (socket-for db path)]
                            (when ws-channel (close! ws-channel))
                            (when buffer-chan (close! buffer-chan)))
                          {:db (assoc-in db [::sockets path] nil)}))

(rf/reg-event-fx ::send (fn [{:keys [db]} [_ path message]]
                         (let [{:keys [buffer-chan state] :as socket-config} (socket-for db path)]
                           (go (>! buffer-chan message))
                           (when false {:dispatch [::log path :buffered (js/Date.) message]})
                           (when (= :initializing state)
                             {:db (update-in db [::sockets path] merge socket-config)}))))

(rf/reg-sub ::state (fn [db [_ path]]
                      (get-in db [::sockets path])))