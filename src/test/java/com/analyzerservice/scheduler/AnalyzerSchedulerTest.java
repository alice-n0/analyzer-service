package com.analyzerservice.scheduler;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.analyzerservice.ai.IncidentAnalyzer;
import com.analyzerservice.detector.AnomalyDetector;
import com.analyzerservice.log.ErrorLogSource;
import com.analyzerservice.metric.SystemMetrics;
import com.analyzerservice.metric.SystemMetricsReader;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AnalyzerSchedulerTest {

    @Mock
    private SystemMetricsReader systemMetricsReader;

    @Mock
    private ErrorLogSource errorLogSource;

    @Mock
    private IncidentAnalyzer incidentAnalyzer;

    private AnalyzerScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new AnalyzerScheduler(
                systemMetricsReader, new AnomalyDetector(), errorLogSource, incidentAnalyzer);
    }

    @Test
    void skipsLogsAndAiWhenHealthy() {
        when(systemMetricsReader.read()).thenReturn(new SystemMetrics(0.1, 0.01, 0.0, true, 0.0, 0.0, 0.0, 0.0));

        scheduler.runAnalysisCycle();

        verify(errorLogSource, never()).collectRecentErrors();
        verify(incidentAnalyzer, never()).analyzeIncident(any(), any());
    }

    @Test
    void collectsLogsAndCallsAnalyzerWhenAnomaly() {
        SystemMetrics hot = new SystemMetrics(1.0, 0.01, 0.0, true, 0.0, 0.0, 0.0, 0.0);
        when(systemMetricsReader.read()).thenReturn(hot);
        when(errorLogSource.collectRecentErrors()).thenReturn(List.of("err"));
        when(incidentAnalyzer.analyzeIncident(eq(hot), eq(List.of("err"))))
                .thenReturn("mock");

        scheduler.runAnalysisCycle();

        verify(incidentAnalyzer).analyzeIncident(eq(hot), eq(List.of("err")));
    }

    @Test
    void nullLogList_replacedWithEmptyList() {
        when(systemMetricsReader.read()).thenReturn(new SystemMetrics(1.0, 0.01, 0.0, true, 0.0, 0.0, 0.0, 0.0));
        when(errorLogSource.collectRecentErrors()).thenReturn(null);
        when(incidentAnalyzer.analyzeIncident(any(), eq(List.of()))).thenReturn("ok");

        scheduler.runAnalysisCycle();

        verify(incidentAnalyzer).analyzeIncident(any(), eq(List.of()));
    }
}
