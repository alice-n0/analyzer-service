package com.analyzerservice.metric;

import java.util.List;

public class PrometheusLabelValuesResponse {

    private String status;
    private List<String> data;

    public String getStatus() {
        return status;
    }

    public List<String> getData() {
        return data;
    }
}