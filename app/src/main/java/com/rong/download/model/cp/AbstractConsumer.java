package com.rong.download.model.cp;

public abstract class AbstractConsumer implements Runnable,Consumer{
    private boolean isRunning = true;

    public abstract void setRunning(boolean running);

    public void setSuperRunning(boolean running){
        this.isRunning = running;
    }
    @Override
    public void run() {
        while (isRunning){
            try {
                consume();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
