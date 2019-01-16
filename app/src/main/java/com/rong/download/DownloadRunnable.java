package com.rong.download;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class DownloadRunnable implements Runnable{

    private final String url;
    private final int runId;
    private final DownloadCallback mDownloadCallback;
    private final DownloadFileCallback mDownloadFileCallback;


    public DownloadRunnable(String url,int runId,DownloadFileCallback downloadFileCallback){
        this.url = url;
        this.runId = runId;
        this.mDownloadCallback = new DownloadCallback();
        this.mDownloadFileCallback = downloadFileCallback;
    }

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
