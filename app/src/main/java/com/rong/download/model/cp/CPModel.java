package com.rong.download.model.cp;

public interface CPModel {
    Runnable createConsumer();
    Runnable createProducer();
}
