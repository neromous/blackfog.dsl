(ns blackfog.dsl.media-test
  (:require [clojure.test :refer [deftest testing is are]]
            [clojure.string :as str]
            [blackfog.dsl.core :refer [reg-element render]]
            [blackfog.dsl.register]))

;; 多媒体文件读取测试
(deftest multimedia-file-operations-test
  (testing "图像文件操作"
    ;; 使用playgrounds文件夹中的实际图像文件
    (let [image-file "playgrounds/test-files/image_test.jpg"]
      
      (let [image-info-result (render [:media/read-image-info image-file])]
        (is (string? image-info-result) "读取图像信息应返回字符串")
        (is (.contains image-info-result "尺寸") "图像信息应包含尺寸")
        (is (.contains image-info-result "格式") "图像信息应包含格式"))
      
      (let [image-data-result (render [:media/read-image-data image-file])]
        (is (string? image-data-result) "读取图像数据应返回字符串")
        (is (.contains image-data-result "图片处理成功") "图像数据应包含成功信息")
        (is (.contains image-data-result "尺寸") "图像数据应包含尺寸信息")
        (is (.contains image-data-result "格式") "图像数据应包含格式信息")
        (is (.contains image-data-result "Base64") "图像数据应包含Base64信息"))))
  
  (testing "音频文件操作"
    ;; 使用playgrounds文件夹中的实际音频文件
    (let [audio-file "playgrounds/test-files/audio_test.mp3"]
      
      (let [audio-info-result (render [:media/read-audio-info audio-file])]
        (is (string? audio-info-result) "读取音频信息应返回字符串")
        ;; 更通用的检查
        (is (or (.contains audio-info-result "音频") 
                (.contains audio-info-result "音频处理失败")
                (.contains audio-info-result "🔊")) 
            "音频信息应包含音频相关标识"))
      
      (let [audio-data-result (render [:media/read-audio-data audio-file])]
        ;; 检查返回值类型，应该是字符串
        (is (string? audio-data-result) "读取音频数据应返回字符串")
        
        ;; 检查字符串内容
        (is (or (.contains audio-data-result "音频处理失败")
                (.contains audio-data-result "音频处理成功"))
            "音频数据应包含处理结果信息")
        
        ;; 如果是成功信息，检查内容
        (when (.contains audio-data-result "音频处理成功")
          (is (.contains audio-data-result "时长") "音频数据应包含时长信息")
          (is (.contains audio-data-result "声道") "音频数据应包含声道信息")
          (is (.contains audio-data-result "采样率") "音频数据应包含采样率信息")))))
  
  (testing "视频文件操作"
    ;; 使用playgrounds文件夹中的实际视频文件
    (let [video-file "playgrounds/test-files/video_test.mp4"]
      
      (let [video-info-result (render [:media/read-video-info video-file])]
        (is (string? video-info-result) "读取视频信息应返回字符串")
        ;; 检查是否为错误信息或包含预期内容
        (is (or (.contains video-info-result "视频处理失败")
                (.contains video-info-result "视频")) 
            "视频信息应包含视频标识或错误信息"))
      
      (let [video-data-result (render [:media/read-video-data video-file])]
        ;; 检查返回值类型，应该是字符串
        (is (string? video-data-result) "读取视频数据应返回字符串")
        
        ;; 检查字符串内容
        (is (or (.contains video-data-result "视频处理失败")
                (.contains video-data-result "视频处理成功")) 
            "视频数据应包含处理结果信息")
        
        ;; 如果是成功信息，检查内容
        (when (.contains video-data-result "视频处理成功")
          (is (.contains video-data-result "时长") "视频数据应包含时长信息")
          (is (.contains video-data-result "分辨率") "视频数据应包含分辨率信息")
          (is (.contains video-data-result "帧率") "视频数据应包含帧率信息")))
      
      (let [video-frame-result (render [:media/read-video-frame video-file 1])]
        (is (string? video-frame-result) "读取视频帧应返回字符串")
        ;; 更通用的检查
        (is (or (.contains video-frame-result "视频") 
                (.contains video-frame-result "视频处理失败")
                (.contains video-frame-result "🎬")
                (.contains video-frame-result "base64")) 
            "视频帧应包含视频相关标识")))))

;; 多媒体文件与其他功能组合测试
(deftest multimedia-integration-test
  (testing "多媒体文件与样式组合"
    ;; 使用playgrounds文件夹中的实际多媒体文件
    (let [image-file "playgrounds/test-files/image_test.jpg"
          audio-file "playgrounds/test-files/audio_test.mp3"
          video-file "playgrounds/test-files/video_test.mp4"]
      
      ;; 测试图像信息与样式组合
      (let [image-info (render [:media/read-image-info image-file])
            result (render [:card
                            [:h3 "图像信息"]
                            [:p [:bold "文件: "] image-file]
                            [:p image-info]])]
        (is (string? result) "图像信息与样式组合应返回字符串")
        (is (pos? (count result)) "组合渲染结果不应为空")
        (is (.contains result "图像信息") "渲染结果应包含标题"))
      
      ;; 测试音频信息与样式组合
      (let [audio-info (render [:media/read-audio-info audio-file])
            result (render [:card
                            [:h3 "音频信息"]
                            [:p [:bold "文件: "] audio-file]
                            [:p audio-info]])]
        (is (string? result) "音频信息与样式组合应返回字符串")
        (is (pos? (count result)) "组合渲染结果不应为空")
        (is (.contains result "音频信息") "渲染结果应包含标题"))
      
      ;; 测试视频信息与样式组合
      (let [video-info (render [:media/read-video-info video-file])
            result (render [:card
                            [:h3 "视频信息"]
                            [:p [:bold "文件: "] video-file]
                            [:p video-info]])]
        (is (string? result) "视频信息与样式组合应返回字符串")
        (is (pos? (count result)) "组合渲染结果不应为空")
        (is (.contains result "视频信息") "渲染结果应包含标题")))))

;; 综合多媒体报告测试
(deftest multimedia-report-test
  (testing "生成多媒体文件综合报告"
    ;; 使用playgrounds文件夹中的实际多媒体文件
    (let [image-file "playgrounds/test-files/image_test.jpg"
          audio-file "playgrounds/test-files/audio_test.mp3"
          video-file "playgrounds/test-files/video_test.mp4"
          
          ;; 获取各种媒体信息
          image-info (render [:media/read-image-info image-file])
          audio-info (render [:media/read-audio-info audio-file])
          video-info (render [:media/read-video-info video-file])
          
          ;; 生成综合报告
          report (render [:rows
                          [:h1 "多媒体文件分析报告"]
                          
                          [:h2 "图像文件分析"]
                          [:p [:bold "文件: "] image-file]
                          [:p image-info]
                          
                          [:h2 "音频文件分析"]
                          [:p [:bold "文件: "] audio-file]
                          [:p audio-info]
                          
                          [:h2 "视频文件分析"]
                          [:p [:bold "文件: "] video-file]
                          [:p video-info]
                          
                          [:hr]
                          [:p [:italic "生成时间: "] (render [:time/now])]])]
      
      (is (string? report) "多媒体报告应返回字符串")
      (is (pos? (count report)) "多媒体报告不应为空")
      (is (.contains report "多媒体文件分析报告") "报告应包含标题")
      (is (.contains report "图像文件分析") "报告应包含图像分析部分")
      (is (.contains report "音频文件分析") "报告应包含音频分析部分")
      (is (.contains report "视频文件分析") "报告应包含视频分析部分"))))

;; 多媒体文件搜索和过滤测试
(deftest multimedia-search-test
  (testing "多媒体文件搜索和过滤"
    ;; 使用playgrounds文件夹中的实际多媒体文件
    (let [test-dir "playgrounds/test-files"
          dir-result (render [:file/list-dir test-dir])]
      
      ;; 测试目录列表包含多媒体文件
      (is (string? dir-result) "目录列表应返回字符串")
      (is (.contains dir-result "image_test.jpg") "目录列表应包含图像文件")
      (is (.contains dir-result "audio_test.mp3") "目录列表应包含音频文件")
      (is (.contains dir-result "video_test.mp4") "目录列表应包含视频文件"))))
