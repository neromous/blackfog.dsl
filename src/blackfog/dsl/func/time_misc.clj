(ns blackfog.dsl.func.time-misc
  (:require [clojure.string :as str]
            [clojure.pprint :as pp])
  (:import [java.time LocalDateTime LocalDate ZonedDateTime ZoneId]
           [java.time.format DateTimeFormatter]
           [java.time.temporal ChronoUnit]))


;; 时间处理核心函数
(def custom-formatter
  (DateTimeFormatter/ofPattern "yyyy-MM-dd HH:mm:ss"))

(defn now []
  (LocalDateTime/now))

(defn to-local-date [^LocalDateTime datetime]
  (.toLocalDate datetime))

(defn start-of-today [^LocalDateTime date]
  (-> date
      to-local-date
      (.atStartOfDay)))

(defn end-of-today [^LocalDateTime date]
  (-> date
      to-local-date
      (.atTime 23 59 59)))

(defn day-of-week [^LocalDateTime date]
  (.getValue (.getDayOfWeek date)))

(defn start-of-week [^LocalDateTime date]
  (let [current-day (day-of-week date)]
    (.minusDays date (dec current-day))))

(defn end-of-week [^LocalDateTime date]
  (let [current-day (day-of-week date)]
    (.plusDays date (- 7 current-day))))

(defn start-of-month [^LocalDateTime date]
  (-> date
      to-local-date
      (.withDayOfMonth 1)
      (.atStartOfDay)))

(defn end-of-month [^LocalDateTime date]
  (let [local-date (to-local-date date)]
    (-> local-date
        (.withDayOfMonth (.lengthOfMonth local-date))
        (.atTime 23 59 59))))

(defn start-of-quarter [^LocalDateTime date]
  (let [month (.getMonthValue date)
        quarter-start-month (inc (* (quot (dec month) 3) 3))]
    (-> date
        (.withMonth quarter-start-month)
        (.withDayOfMonth 1)
        (.withHour 0)
        (.withMinute 0)
        (.withSecond 0))))

(defn end-of-quarter [^LocalDateTime date]
  (-> (start-of-quarter date)
      (.plusMonths 3)
      (.minusNanos 1)))

(defn days-between [^LocalDateTime start ^LocalDateTime end]
  (.between ChronoUnit/DAYS start end))

;; 时间戳函数
(defn timestamp []
  (.format (now) custom-formatter))

(defn yesterday-timestamp []
  (.format (.minusDays (now) 1) custom-formatter))

(defn tomorrow []
  (.format (.plusDays (now) 1) custom-formatter))

(defn this-week []
  (let [now (now)
        start (start-of-week now)
        end (end-of-week now)]
    [(.format start custom-formatter)
     (.format end custom-formatter)]))

(defn this-month []
  (let [now (now)
        start (start-of-month now)
        end (end-of-month now)]
    [(.format start custom-formatter)
     (.format end custom-formatter)]))
