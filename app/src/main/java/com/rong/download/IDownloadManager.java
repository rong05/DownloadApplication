package com.rong.download;

public interface IDownloadManager {
     /**
      * 下载文件设置
      * @param url 下载地址
      * @param md5 md5校验
      * @param length 文件长度
      * @param savePath 保存地址
      * @param fileName
      */
     void downloadFile(final String url,final String md5,final long length,
                             final String savePath,final String fileName);

     void setMaxCacheFile(long maxSize);

     void startDownload();

     void onFailure(String url,Exception e);

     void onResponse(String url);

     void onComplete(String url);

     void onDestroy();
}
