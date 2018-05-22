# VideoSplitDemo
视频裁剪并提取帧图片demo，
在 https://github.com/ta893115871/VideoEdit 基础上增加了视频裁剪和画面提取， 
视频裁剪利用了https://github.com/sannies/mp4parser,
因为系统自带的MediaMetadataRetriever会出现提出帧为null并且速度比较慢，所以改采用
FFMpegMediaMetadataRetriever提取帧
