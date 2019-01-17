package com.rong.download;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

/**
 * 文件下载的执行者
 */
public class DownloadExecutor implements Runnable{

    private final String url;
    private final int runId;
    private final DownloadCallback mDownloadCallback;
    private final DownloadFileCallback mDownloadFileCallback;


    public DownloadExecutor(String url, int runId, DownloadFileCallback downloadFileCallback){
        this.url = url;
        this.runId = runId;
        this.mDownloadCallback = new DownloadCallback();
        this.mDownloadFileCallback = downloadFileCallback;
    }

    /**
     * 文件下载回调
     */
    public interface DownloadFileCallback{
        void onFailure(IOException e);
        void onResponse(String url,int runId);
    }

    @Override
    public void run() {
        try {
            HttpUtils.getInstance().downloadAsyncFile(url,mDownloadCallback);
        } catch (IOException e) {
            //e.printStackTrace();
            mDownloadFileCallback.onFailure(e);
        }
    }


    /**
     * okhttp回调接口
     */
    private class DownloadCallback implements Callback{

        @Override
        public void onFailure(Call call, IOException e) {
            mDownloadFileCallback.onFailure(e);
        }

        @Override
        public void onResponse(Call call, Response response) throws IOException {
            mDownloadFileCallback.onResponse(url,runId);
        }
    }
}
