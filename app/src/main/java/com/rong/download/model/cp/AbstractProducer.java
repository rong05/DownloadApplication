package com.rong.download.model.cp;

public abstract class AbstractProducer implements Runnable,Producer {
    @Override
    public void run() {
        try {
            produce();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
