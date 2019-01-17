package com.rong.download;

import com.rong.download.model.cp.AbstractConsumer;
import com.rong.download.model.cp.AbstractProducer;
import com.rong.download.model.cp.CPModel;
import com.rong.download.model.cp.Consumer;
import com.rong.download.model.cp.Producer;

import java.util.concurrent.BlockingDeque;

public final class DownloadManagerGlobal implements CPModel {


    private BlockingDeque<FileTmp> fileTmpQueue = null;

    private DownloadManagerGlobal(){

    }

    @Override
    public Runnable creatConsumer() {
        return null;
    }

    @Override
    public Runnable creatProducer() {
        return null;
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

        }
    }

    /**
     * 下载文件的生产者
     */
    private final class ProducerDownloadImpl extends AbstractProducer implements Producer,Runnable{

        @Override
        public void produce() throws InterruptedException {

        }
    }

}
