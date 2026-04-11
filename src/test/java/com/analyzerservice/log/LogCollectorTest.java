package com.analyzerservice.log;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.analyzerservice.config.LokiProperties;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

class LogCollectorTest {

    private MockRestServiceServer server;
    private LogCollector collector;

    @BeforeEach
    void setUp() {
        RestTemplate restTemplate = new RestTemplate();
        server = MockRestServiceServer.bindTo(restTemplate).build();

        LokiProperties props = new LokiProperties();
        props.setUrl("http://localhost:3100");
        props.setQueryRangePath("/loki/api/v1/query_range");
        props.setErrorLogQuery("{job=\"msa\"} |= \"error\"");
        props.setLookbackSeconds(60);
        props.setMaxLines(10);
        collector = new LogCollector(restTemplate, props);
    }

    @AfterEach
    void tearDown() {
        server.verify();
    }

    @Test
    void collectRecentErrors_parsesStreams_sortsNewestFirst_capsAtTen() {
        String json =
                """
                {
                  "status":"success",
                  "data":{
                    "resultType":"streams",
                    "result":[
                      {
                        "stream":{"job":"msa"},
                        "values":[
                          ["1000000000000000001","older error"],
                          ["3000000000000000003","newest error"],
                          ["2000000000000000002","middle error"]
                        ]
                      }
                    ]
                  }
                }
                """;
        server.expect(requestTo(Matchers.containsString("/loki/api/v1/query_range")))
                .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));

        assertThat(collector.collectRecentErrors())
                .containsExactly("newest error", "middle error", "older error");
    }

    @Test
    void collectRecentErrors_returnsAtMostTenLines() {
        StringBuilder values = new StringBuilder();
        for (int i = 0; i < 12; i++) {
            if (i > 0) {
                values.append(',');
            }
            values.append('[')
                    .append('"')
                    .append(i + 1)
                    .append("000000000000000000\"")
                    .append(',')
                    .append('"')
                    .append("line-")
                    .append(i)
                    .append('"')
                    .append(']');
        }
        String json =
                """
                {"status":"success","data":{"resultType":"streams","result":[{"stream":{"job":"msa"},"values":["""
                        + values
                        + """
                ]}]}}
                """;
        server.expect(requestTo(Matchers.containsString("/loki/api/v1/query_range")))
                .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));

        assertThat(collector.collectRecentErrors()).hasSize(10);
    }

    @Test
    void collectRecentErrors_returnsEmptyWhenNotSuccess() {
        server.expect(requestTo(Matchers.containsString("/loki/api/v1/query_range")))
                .andRespond(withSuccess("{\"status\":\"fail\"}", MediaType.APPLICATION_JSON));

        assertThat(collector.collectRecentErrors()).isEmpty();
    }
}
