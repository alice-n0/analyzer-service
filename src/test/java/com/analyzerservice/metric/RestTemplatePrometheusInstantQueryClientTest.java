package com.analyzerservice.metric;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.analyzerservice.config.PrometheusProperties;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

class RestTemplatePrometheusInstantQueryClientTest {

    private MockRestServiceServer server;
    private RestTemplatePrometheusInstantQueryClient client;

    @BeforeEach
    void setUp() {
        RestTemplate restTemplate = new RestTemplate();
        server = MockRestServiceServer.bindTo(restTemplate).build();

        PrometheusProperties props = new PrometheusProperties();
        props.setUrl("http://localhost:9090");
        client = new RestTemplatePrometheusInstantQueryClient(restTemplate, props);
    }

    @AfterEach
    void tearDown() {
        server.verify();
    }

    @Test
    void queryInstantScalar_returnsFirstSampleValue() {
        String json =
                """
                {"status":"success","data":{"result":[{"metric":{},"value":[1730000000,"0.42"]}]}}
                """;
        server.expect(requestTo(Matchers.containsString("/api/v1/query")))
                .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));

        assertThat(client.queryInstantScalar("up")).isEqualTo(0.42);
    }

    @Test
    void queryInstantScalar_returnsZeroWhenEmptyResult() {
        String json = "{\"status\":\"success\",\"data\":{\"result\":[]}}";
        server.expect(requestTo(Matchers.containsString("/api/v1/query")))
                .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));

        assertThat(client.queryInstantScalar("empty()")).isZero();
    }

    @Test
    void queryInstantScalar_returnsZeroForBlankPromql() {
        assertThat(client.queryInstantScalar("  ")).isZero();
    }
}
