package com.rong.download;

import android.support.v4.text.TextUtilsCompat;
import com.rong.download.model.cp.AbstractConsumer;
import com.rong.download.model.cp.AbstractProducer;
import com.rong.download.model.cp.CPModel;
import com.rong.download.model.cp.Consumer;
import com.rong.download.model.cp.Producer;
import okhttp3.Response;

import java.io.*;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingDeque;

public final class DownloadManagerGlobal implements CPModel,IDownloadManager{


    private final BlockingDeque<FileTmp> fileTmpQueue;
    private final BlockingDeque<FileTask> fileTaskDeque;
    private DownloadFileListener mDownloadFileListener;
    private  static volatile DownloadManagerGlobal sDownloadManagerGlobal = null;
    private final ConcurrentHashMap<String,FileTask> fileTaskMap;
    private long maxCacheFileSize = 1024l * 1024l;

    private DownloadManagerGlobal(){
        fileTmpQueue = new LinkedBlockingDeque<>();
        fileTaskDeque = new LinkedBlockingDeque<>();
        fileTaskMap = new ConcurrentHashMap<>();
    }

    public static DownloadManagerGlobal instance(){
        if(sDownloadManagerGlobal == null){
            synchronized (DownloadManagerGlobal.class){
                if(sDownloadManagerGlobal == null){
                    sDownloadManagerGlobal = new DownloadManagerGlobal();
                }
            }
        }
        return sDownloadManagerGlobal;
    }

    @Override
    public ConsumerDownloadImpl createConsumer() {
        return new ConsumerDownloadImpl();
    }

    public Runnable createProducer(FileTmp fileTmp) {
        return new ProducerDownloadImpl(fileTmp);
    }
    @Override
    public Runnable createProducer() {
        return new ProducerDownloadImpl(null);
    }

    @Override
    public void downloadFile(String url, String md5, long length, String savePath, String fileName) {
        if(url == null ||"".equals(url.trim()) || fileTaskMap.containsKey(url.trim())){
            return;
        }
        final  FileTask fileTask = new FileTask(url,md5,length,savePath,fileName);
        fileTaskMap.put(url.trim(),fileTask);
        fileTaskDeque.offer(fileTask);
    }

    @Override
    public void setMaxCacheFile(long maxSize) {
        this.maxCacheFileSize = maxSize;
    }

    @Override
    public void startDownload() {
        FileTask fileTask = fileTaskDeque.peek();
        if(fileTask != null){
            long endFileSize = 0l;
            int threadSize = (int) (fileTask.length / maxCacheFileSize);
            final long x = fileTask.length%maxCacheFileSize;
            if(x > 0){
                threadSize += 1;
                endFileSize = x;
            }
            for(int i =0;i < threadSize ; i++){
                long startPath ;
                long endPath ;
                if(i != threadSize -1){
                    startPath = i * maxCacheFileSize;
                    endPath = startPath + maxCacheFileSize;
                }else {
                    startPath = i * maxCacheFileSize;
                    endPath = startPath + endFileSize;
                }
                final String downloadUrl = fileTask.url + "" + startPath+ "" + endPath;
                final String fileName  = fileTask.fileName + "_"+ i + "_tmp";
                FileTmp fileTmp = new FileTmp(startPath,endPath,fileName,downloadUrl,fileTask.url,i,fileTask.savePath);
                ThreadManager.getPoolProxy().execute(createProducer(fileTmp));
            }
        }
    }

    @Override
    public void setDownloadFileListener(DownloadFileListener listener){
        this.mDownloadFileListener = listener;
    }

    public void onFailure(String url, Exception e) {

    }


    public void onResponse(String url) {

    }


    public void onComplete(String url) {

    }

    @Override
    public void onDestroy() {

    }

    /**
     * 下载文件的消费者
     */
    private  final class ConsumerDownloadImpl extends AbstractConsumer implements Consumer ,Runnable {

        @Override
        public void setRunning(boolean running) {
            super.setSuperRunning(running);
        }

        @Override
        public void consume() throws InterruptedException {
           final FileTmp fileTmp = fileTmpQueue.take();
            if(fileTmp != null && fileTaskMap.containsKey(fileTmp.tagUrl)) {
               final FileTask fileTask =  fileTaskMap.get(fileTmp.tagUrl);
               if(fileTask != null){
                   addCacheFile(fileTask,fileTmp);
               }
            }
            File file = new File(fileTmp.savePath,fileTmp.fileName);
            if(file.exists()){
                file.delete();
            }
        }
    }


    private void addCacheFile(FileTask fileTask,FileTmp fileTmp){
        File sourceFile = new File(fileTmp.savePath,fileTmp.fileName);
        if(sourceFile.exists()) {
            boolean isAddSuccess = false;
            File targetFile = new File(fileTask.savePath, fileTask.fileName);
            if (!targetFile.exists()) {
                try {
                    targetFile.createNewFile();
                } catch (IOException e) {
                    e.printStackTrace();
                    isAddSuccess = false;
                    synchronized (sDownloadManagerGlobal) {
                        sDownloadManagerGlobal.onFailure(fileTmp.downloadUrl,e);
                    }
                    return;
                }
            }
            FileInputStream fis = null;
            byte[] buf=new byte[2048];
            int len = 0;
            RandomAccessFile target = null;
            try {
                target =   new RandomAccessFile(targetFile, "rw");
                fis = new FileInputStream(sourceFile);
                long sum=0;
                final long total=sourceFile.length();
                target.seek(fileTmp.startPath);
                while((len = fis.read(buf))!=-1){
                    target.write(buf);
                    sum+=len;
                    int progress=(int)(sum*1.0f/total*100);
                    //下载中
                }
                isAddSuccess = true;
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                synchronized (sDownloadManagerGlobal) {
                    sDownloadManagerGlobal.onFailure(fileTmp.downloadUrl,e);
                }
                isAddSuccess = false;
            } catch (IOException e) {
                e.printStackTrace();
                synchronized (sDownloadManagerGlobal) {
                    sDownloadManagerGlobal.onFailure(fileTmp.downloadUrl,e);
                }
                isAddSuccess = false;
            }finally {
                try {
                    if(target != null){
                        target.close();
                    }
                    if (fis != null){
                        fis.close();
                    }
                }catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if(isAddSuccess == true){
                final long countLength = fileTask.getCountLength() + (fileTmp.endPath - fileTmp.startPath);
                if(sourceFile.exists()){
                    sourceFile.delete();
                }
                fileTaskMap.remove(fileTask.url);
                if(countLength == fileTask.length){
                    if(judgeMD5(targetFile,fileTask.md5)){
                        synchronized (sDownloadManagerGlobal) {
                            sDownloadManagerGlobal.onComplete(fileTask.url);
                        }
                    }else {
                        if(targetFile.exists()){
                            targetFile.delete();
                        }
                        synchronized (sDownloadManagerGlobal) {
                            sDownloadManagerGlobal.onFailure(fileTask.url,new IllegalArgumentException("The content of the file is incorrect"));
                        }
                    }
                }else {
                    fileTask.setCountLength(countLength);
                    fileTaskMap.put(fileTask.url,fileTask);
                }
            }
        }
    }

    private boolean judgeMD5(File file,String md5){
        return false;
    }

    /**
     * 下载文件的生产者
     */
    private final class ProducerDownloadImpl extends AbstractProducer implements Producer,Runnable{

        final FileTmp fileTmp;

        public ProducerDownloadImpl(FileTmp fileTmp) {
            if(fileTmp == null){
                throw new NullPointerException("DownloadExecutor ProducerDownloadImpl is null");
            }
            this.fileTmp = fileTmp;
        }

        @Override
        public void produce() throws InterruptedException {
           final DownloadExecutor executor = new DownloadExecutor.Builder()
                    .setUrl(fileTmp.downloadUrl)
                    .setRunId(fileTmp.runId)
                    .setDownloadFileCallback(new DownloadExecutor.DownloadFileCallback() {
                        @Override
                        public void onFailure(IOException e) {
                            synchronized (sDownloadManagerGlobal) {
                                sDownloadManagerGlobal.onFailure(fileTmp.downloadUrl,e);
                            }
                        }

                        @Override
                        public void onResponse(String url, int runId, Response response) {
                            File saveFile = new File(fileTmp.savePath);
                            if(!saveFile.exists()){
                                saveFile.mkdirs();
                            }
                            File file = new File(fileTmp.savePath,fileTmp.fileName);
                            if(!file.exists()){
                                try {
                                    file.createNewFile();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                    synchronized (sDownloadManagerGlobal) {
                                        sDownloadManagerGlobal.onFailure(fileTmp.downloadUrl,e);
                                    }
                                }
                            }
                            FileOutputStream fos = null;
                            InputStream is=null;
                            byte[] buf=new byte[2048];
                            int len=0;
                            try{
                                is=response.body().byteStream();
                                final long total=response.body().contentLength();
                                fos=new FileOutputStream(file);
                                long sum=0;
                                while((len = is.read(buf))!=-1){
                                    fos.write(buf,0,len);
                                    sum+=len;
                                    int progress=(int)(sum*1.0f/total*100);
                                    //下载中

                                }
                                fos.flush();
                                //下载完成
                                fileTmpQueue.put(fileTmp);
                                synchronized (sDownloadManagerGlobal) {
                                    sDownloadManagerGlobal.onResponse(fileTmp.downloadUrl);
                                }
                            }catch (Exception e){
                                synchronized (sDownloadManagerGlobal) {
                                    sDownloadManagerGlobal.onFailure(fileTmp.downloadUrl,e);
                                }
                            }finally{
                                try{
                                    if(is!=null)
                                        is.close();
                                }catch (IOException e){

                                }
                                try {
                                    if(fos!=null){
                                        fos.close();
                                    }
                                }catch (IOException e){

                                }
                            }
                        }
                    }).run();
        }
    }

}
