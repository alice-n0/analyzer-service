package com.analyzerservice.log;

import java.util.List;

/**
 * 장애 분석에 사용할 에러 로그 라인을 제공합니다.
 */
public interface ErrorLogSource {

    /**
     * @return 비-null 목록(비어 있을 수 있음). 구현체는 방어적 복사된 목록을 반환하는 것을 권장합니다.
     */
    List<String> collectRecentErrors();
}
