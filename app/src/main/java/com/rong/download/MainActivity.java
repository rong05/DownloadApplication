package com.rong.download;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

public class MainActivity extends AppCompatActivity {

    private LruCache<Integer,Integer> lruCache;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        lruCache = new LruCache<>(6);
        lruCache.put(1,11);
        lruCache.put(2,11);
        lruCache.put(3,11);
        lruCache.put(4,11);
        lruCache.put(5,11);
        lruCache.put(6,11);
        Log.d("mm",lruCache.toAllValueString());
        lruCache.get(3);
        lruCache.put(5,66);
        lruCache.put(7,88);
        lruCache.get(2);
        lruCache.put(8,11);
        Log.d("mm",lruCache.toAllValueString());
    }
}
