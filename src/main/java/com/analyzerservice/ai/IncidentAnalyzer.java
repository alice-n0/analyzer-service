package com.analyzerservice.ai;

import com.analyzerservice.metric.SystemMetrics;
import java.util.List;

/**
 * 메트릭·로그를 바탕으로 장애 분석 결과(또는 mock 응답)를 반환합니다.
 */
public interface IncidentAnalyzer {

    String analyzeIncident(SystemMetrics metrics, List<String> errorLogs);
}
