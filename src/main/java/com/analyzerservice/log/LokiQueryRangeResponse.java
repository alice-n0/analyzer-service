package com.analyzerservice.log;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

/**
 * {@code GET /loki/api/v1/query_range} JSON 응답의 streams 결과용 모델입니다.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class LokiQueryRangeResponse {

    private String status;

    @JsonProperty("data")
    private LokiData data;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LokiData getData() {
        return data;
    }

    public void setData(LokiData data) {
        this.data = data;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class LokiData {

        @JsonProperty("resultType")
        private String resultType;

        private List<LokiStreamResult> result;

        public String getResultType() {
            return resultType;
        }

        public void setResultType(String resultType) {
            this.resultType = resultType;
        }

        public List<LokiStreamResult> getResult() {
            return result;
        }

        public void setResult(List<LokiStreamResult> result) {
            this.result = result;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class LokiStreamResult {

        private Map<String, String> stream;

        /**
         * 각 원소는 [나노초 타임스탬프 문자열, 로그 라인] 입니다.
         */
        private List<List<String>> values;

        public Map<String, String> getStream() {
            return stream;
        }

        public void setStream(Map<String, String> stream) {
            this.stream = stream;
        }

        public List<List<String>> getValues() {
            return values;
        }

        public void setValues(List<List<String>> values) {
            this.values = values;
        }
    }
}
