(ns blackfog.dsl.func.media
  (:require [blackfog.dsl.core :refer [reg-element]]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.data.json :as json]
            [clojure.edn :as edn]
            [clojure.data.csv :as csv]
            [clojure.set :as set]
            [blackfog.dsl.func.file :refer [*file-exists? *get-file-type *get-file-info]])
  (:import [java.io File ByteArrayOutputStream]
           [java.nio.file Files Paths]
           [java.util Base64]
           [java.awt.image BufferedImage]
           [javax.imageio ImageIO]
           [javax.sound.sampled AudioSystem AudioFormat AudioInputStream]
           [org.apache.tika Tika]
           [org.jcodec.api FrameGrab JCodecException]
           [org.jcodec.common.model Picture]
           [org.jcodec.scale AWTUtil]
           [org.jcodec.containers.mp4.demuxer MP4Demuxer]
           [org.jcodec.common.io NIOUtils]
           [org.jcodec.common DemuxerTrack]
           [org.jcodec.common.model Packet]))

;; ================ 二进制文件操作 ================
(defn *file-to-base64
  "将文件转换为Base64编码"
  [path]
  (try
    (let [bytes (Files/readAllBytes (Paths/get path (into-array String [])))]
      (.encodeToString (Base64/getEncoder) bytes))
    (catch Exception e
      (str "❌ 文件编码失败: " (.getMessage e)))))

(defn *get-image-info
  "获取图片信息"
  [path]
  (try
    (let [image (ImageIO/read (io/file path))
          width (.getWidth image)
          height (.getHeight image)]
      {:width width
       :height height
       :format (last (str/split path #"\."))})
    (catch Exception e
      (str "❌ 图片信息获取失败: " (.getMessage e)))))

(defn read-image-file-info [path]
  (if (*file-exists? path)
    (let [file-type (*get-file-type path)]
      (if (str/starts-with? file-type "image/")
        (let [image-info (*get-image-info path)
              base64 (*file-to-base64 path)]
          (if (map? image-info)
            (str "🖼️ 图片: " (-> path io/file .getName) "\n"
                 "📊 尺寸: " (:width image-info) " x " (:height image-info) " 像素\n"
                 "📄 格式: " (:format image-info) "\n\n"
                 "📝 Base64编码 (前100字符):\n"
                 (subs base64 0 (min 100 (count base64))) "...")
            (str "❌ 图片信息获取失败: " image-info)))
        (str "❌ 文件不是图片: " path)))
    (str "❌ 文件不存在: " path)))

(defn *get-audio-info
  "获取音频信息"
  [path]
  (try
    (let [audio-file (AudioSystem/getAudioFileFormat (io/file path))
          format (.getFormat audio-file)
          frame-length (.getFrameLength audio-file)
          duration (/ frame-length (.getSampleRate format))
          channels (.getChannels format)
          sample-rate (.getSampleRate format)]
      {:duration duration
       :channels channels
       :sample-rate sample-rate
       :format (last (str/split path #"\."))})
    (catch Exception e
      (str "❌ 音频信息获取失败: " (.getMessage e)))))

(defn read-audio-file-info [path]
  (if (*file-exists? path)
    (let [file-type (*get-file-type path)]
      (if (str/starts-with? file-type "audio/")
        (let [audio-info (*get-audio-info path)]
          (if (map? audio-info)
            (str "🔊 音频: " (-> path io/file .getName) "\n"
                 "⏱️ 时长: " (format "%.2f" (:duration audio-info)) " 秒\n"
                 "🎛️ 声道: " (:channels audio-info) "\n"
                 "📊 采样率: " (:sample-rate audio-info) " Hz\n"
                 "📄 格式: " (:format audio-info))
            (str "❌ 音频信息获取失败: " audio-info)))
        (str "❌ 文件不是音频: " path)))
    (str "❌ 文件不存在: " path)))


;; ================ 增强的媒体处理函数 ================

(defn- *extract-image-data
  "提取图片的完整数据，包括元数据和图片内容"
  [path]
  (try
    (let [file (io/file path)
          image (ImageIO/read file)
          width (.getWidth image)
          height (.getHeight image)
          format (last (str/split path #"\."))
          ;; 转换图片为字节数组
          baos (ByteArrayOutputStream.)
          _ (ImageIO/write image format baos)
          image-bytes (.toByteArray baos)
          base64-data (.encodeToString (Base64/getEncoder) image-bytes)
          ;; 获取更多图片属性
          color-model (.getColorModel image)
          pixel-size (.getPixelSize color-model)
          transparency (.getTransparency color-model)
          has-alpha (.hasAlpha color-model)]
      {:metadata {:width width
                 :height height
                 :format format
                 :file-size (.length file)
                 :last-modified (.lastModified file)
                 :color-depth pixel-size
                 :transparency transparency
                 :has-alpha has-alpha
                 :mime-type (*get-file-type path)}
       :data {:base64 base64-data
              :dimensions [width height]}})
    (catch Exception e
      (str "❌ 图片处理失败: " (.getMessage e)))))

(defn read-image-data [path]
  (if (*file-exists? path)
    (let [file-type (*get-file-type path)]
      (if (str/starts-with? file-type "image/")
        (let [result (*extract-image-data path)]
          (if (string? result)
            result  ;; 已经是错误字符串
            (let [metadata (:metadata result)
                  data (:data result)
                  base64 (:base64 data)]
              (str "🖼️ 图片处理成功:\n"
                   "📊 尺寸: " (:width metadata) " x " (:height metadata) " 像素\n"
                   "📄 格式: " (:format metadata) "\n"
                   "🎨 色彩深度: " (:color-depth metadata) " 位\n"
                   "📝 Base64编码 (前100字符):\n"
                   (subs base64 0 (min 100 (count base64))) "..."))))
        (str "❌ 文件不是图片: " path)))
    (str "❌ 文件不存在: " path)))

(defn- *extract-audio-data
  "提取音频的完整数据，包括元数据和音频内容"
  [path]
  (try
    (let [file (io/file path)
          audio-stream (AudioSystem/getAudioInputStream file)
          format (.getFormat audio-stream)
          frame-length (.getFrameLength audio-stream)
          duration (/ frame-length (.getSampleRate format))
          ;; 读取音频数据
          byte-array (byte-array (.available audio-stream))
          _ (.read audio-stream byte-array)
          base64-data (.encodeToString (Base64/getEncoder) byte-array)]
      {:metadata {:duration duration
                 :channels (.getChannels format)
                 :sample-rate (.getSampleRate format)
                 :sample-size-bits (.getSampleSizeInBits format)
                 :frame-rate (.getFrameRate format)
                 :frame-size (.getFrameSize format)
                 :encoding (str (.getEncoding format))
                 :file-size (.length file)
                 :last-modified (.lastModified file)
                 :format (last (str/split path #"\."))
                 :mime-type (*get-file-type path)}
       :data {:base64 base64-data
              :duration duration
              :sample-count frame-length}})
    (catch Exception e
      (str "❌ 音频处理失败: " (.getMessage e)))))


(defn read-audio-data [path]
  (if (*file-exists? path)
    (let [file-type (*get-file-type path)]
      (if (str/starts-with? file-type "audio/")
        (let [result (*extract-audio-data path)]
          (if (string? result)
            result  ;; 已经是错误字符串
            (let [metadata (:metadata result)]
              (str "🔊 音频处理成功:\n"
                   "⏱️ 时长: " (format "%.2f" (:duration metadata)) " 秒\n"
                   "🎛️ 声道: " (:channels metadata) "\n"
                   "📊 采样率: " (:sample-rate metadata) " Hz\n"
                   "🎚️ 采样位数: " (:sample-size-bits metadata) " 位\n"
                   "📄 格式: " (:format metadata)))))
        (str "❌ 文件不是音频: " path)))
    (str "❌ 文件不存在: " path)))


(defn *extract-video-frame
  "从视频中提取指定时间点的帧"
  [path time-sec]
  (try
    (let [file (io/file path)
          grab (FrameGrab/createFrameGrab (NIOUtils/readableChannel file))
          _ (.seekToSecondPrecise grab time-sec)
          frame (.getNativeFrame grab)
          buffered-image (AWTUtil/toBufferedImage frame)
          baos (ByteArrayOutputStream.)
          _ (ImageIO/write buffered-image "png" baos)
          frame-bytes (.toByteArray baos)]
      {:base64 (.encodeToString (Base64/getEncoder) frame-bytes)
       :width (.getWidth buffered-image)
       :height (.getHeight buffered-image)})
    (catch Exception e
      (str "❌ 视频帧提取失败: " (.getMessage e)))))

(defn- *extract-video-data
  "提取视频的完整数据，包括元数据和关键帧"
  [path]
  (try
    (let [file (io/file path)
          channel (NIOUtils/readableChannel file)
          demuxer (MP4Demuxer/createMP4Demuxer channel)
          video-track (.getVideoTrack demuxer)
          ;; 获取视频基本信息
          meta (.getMeta video-track)
          total-frames (.getFrameCount video-track)
          duration (.getTotalDuration video-track)
          ;; 提取关键帧（这里提取开始、中间和结束的帧）
          frames {:start (*extract-video-frame path 0)
                 :middle (*extract-video-frame path (/ duration 2))
                 :end (*extract-video-frame path (- duration 1))}]
      {:metadata {:duration duration
                 :total-frames total-frames
                 :frame-rate (.getFPS meta)
                 :width (.getWidth meta)
                 :height (.getHeight meta)
                 :file-size (.length file)
                 :last-modified (.lastModified file)
                 :format (last (str/split path #"\."))
                 :mime-type (*get-file-type path)}
       :data {:key-frames frames
              :duration duration}})
    (catch Exception e
      (str "❌ 视频处理失败: " (.getMessage e)))))

(defn read-video-file-info [path]
  (if (*file-exists? path)
    (let [file-type (*get-file-type path)
          file-info (*get-file-info path)]
      (if (str/starts-with? file-type "video/")
        (str "🎬 视频: " (:name file-info) "\n"
             "📊 大小: " (:size file-info) " 字节\n"
             "📄 格式: " (last (str/split path #"\.")) "\n"
             "📝 MIME类型: " file-type)
        (str "❌ 文件不是视频: " path)))
    (str "❌ 文件不存在: " path)))


(defn read-video-data [path]
  (if (*file-exists? path)
    (let [file-type (*get-file-type path)]
      (if (str/starts-with? file-type "video/")
        (let [result (*extract-video-data path)]
          (if (string? result)
            result  ;; 已经是错误字符串
            (let [metadata (:metadata result)]
              (str "🎬 视频处理成功:\n"
                   "⏱️ 时长: " (:duration metadata) " 秒\n"
                   "📊 总帧数: " (:total-frames metadata) "\n"
                   "🖼️ 分辨率: " (:width metadata) " x " (:height metadata) " 像素\n"
                   "⏲️ 帧率: " (:frame-rate metadata) " fps\n"
                   "📄 格式: " (:format metadata)))))
        (str "❌ 文件不是视频: " path)))
    (str "❌ 文件不存在: " path)))

 (defn read-video-frame [path time-sec]
  (if (*file-exists? path)
    (let [file-type (*get-file-type path)]
      (if (str/starts-with? file-type "video/")
        (let [result (*extract-video-frame path time-sec)]
          (if (string? result)
            result  ;; 已经是错误字符串
            (str "🎬 视频帧提取成功:\n"
                 "📊 尺寸: " (:width result) " x " (:height result) " 像素\n"
                 "⏱️ 时间点: " time-sec " 秒\n"
                 "📝 Base64编码 (前100字符):\n"
                 (subs (:base64 result) 0 (min 100 (count (:base64 result)))) "...")))
        (str "❌ 文件不是视频: " path)))
    (str "❌ 文件不存在: " path)))
 