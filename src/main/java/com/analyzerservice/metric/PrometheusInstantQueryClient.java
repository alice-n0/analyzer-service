package com.analyzerservice.metric;

import java.util.List;

/**
 * Prometheus instant query API에서 단일 스칼라 값만 조회합니다.
 */
public interface PrometheusInstantQueryClient {

    /**
     * @return 첫 시계열 샘플의 숫자 값, 없거나 실패 시 {@code 0.0}
     */
    double queryInstantScalar(String promql);
    List<String> queryLabelValues(String metric, String label);

}
