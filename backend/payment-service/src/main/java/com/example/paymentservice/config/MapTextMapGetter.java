package com.example.paymentservice.config;


import io.opentelemetry.context.propagation.TextMapGetter;

import java.util.Map;

public class MapTextMapGetter implements TextMapGetter<Map<String, String>> {

    public static final MapTextMapGetter INSTANCE = new MapTextMapGetter();

    @Override
    public Iterable<String> keys(Map<String, String> carrier) {
        return carrier.keySet();
    }

    @Override
    public String get(Map<String, String> carrier, String key) {
        return carrier.get(key);
    }
}