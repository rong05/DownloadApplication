package com.rong.download;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

/**
 * 文件下载的执行者
 */
public final class DownloadExecutor {

    private final String url;
    private final int runId;
    private final DownloadCallback mDownloadCallback;
    private final DownloadFileCallback mDownloadFileCallback;


    protected DownloadExecutor(String url, int runId, DownloadFileCallback downloadFileCallback){
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
        void onResponse(String url,int runId,Response response);
    }

    protected void run() {
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
            mDownloadFileCallback.onResponse(url,runId,response);
        }
    }


    public final static class Builder{

        private  String url;
        private  int runId = -1;
        private  DownloadFileCallback mDownloadFileCallback;

        public Builder(){
        }

        public DownloadExecutor.Builder setUrl(String url) {
            this.url = url;
            return this;
        }

        public DownloadExecutor.Builder setRunId(int runId) {
            this.runId = runId;
            return this;
        }

        public DownloadExecutor.Builder setDownloadFileCallback(DownloadFileCallback mDownloadFileCallback) {
            this.mDownloadFileCallback = mDownloadFileCallback;
            return this;
        }

        public DownloadExecutor create() {
            if(url == null || "".equals(url)){
                throw new NullPointerException("DownloadExecutor url is null");
            }
            if(mDownloadFileCallback == null){
                throw new NullPointerException("DownloadExecutor mDownloadFileCallback is null");
            }
            if(runId == -1){
                throw new NullPointerException("DownloadExecutor runId is -1");
            }

            DownloadExecutor executor = new DownloadExecutor(url,runId,mDownloadFileCallback);
            return executor;
        }

        public DownloadExecutor run(){
            DownloadExecutor executor = this.create();
            executor.run();
            return executor;
        }
    }
}
