package com.badminton.service.analysis;

import com.badminton.entity.analysis.AiAnalysis;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AnalysisProgressTest {

    private final MockAiEngineService mock = mock(MockAiEngineService.class);
    private final AnalysisProgress progress = new AnalysisProgress(mock);

    private AiAnalysis processing() {
        when(mock.getProgress(any())).thenReturn(0);
        AiAnalysis analysis = new AiAnalysis();
        analysis.setId(1L);
        analysis.setStatus("PROCESSING");
        return analysis;
    }

    @Test
    void theBarMovesWhileTheVideoDownloads() {
        AiAnalysis analysis = processing();
        analysis.setWorkerStage("downloading");
        analysis.setWorkerStagePercent((short) 50);
        AnalysisProgress.Progress p = progress.of(analysis, null);
        assertThat(p.percent()).isEqualTo(4);
        assertThat(p.stage()).isEqualTo("downloading: Đang tải video (50%)");
    }

    @Test
    void setUpStepsComeInOrderBeforeAnalysis() {
        assertThat(AnalysisProgress.setup(null, null).percent()).isZero();
        assertThat(AnalysisProgress.setup("downloading", (short) 100).percent()).isEqualTo(8);
        assertThat(AnalysisProgress.setup("cutting", null).percent()).isEqualTo(9);
        assertThat(AnalysisProgress.setup("loading", null).percent()).isEqualTo(AnalysisProgress.SETUP_PERCENT);
    }

    @Test
    void analysedVideoFillsTheRestOfTheBar() {
        AiAnalysis analysis = processing();
        analysis.setWorkerStage("loading");
        analysis.setAnalyzedUntilSeconds(BigDecimal.valueOf(0));
        analysis.setVideoDurationSeconds(BigDecimal.valueOf(600));
        assertThat(progress.of(analysis, List.of()).percent()).isEqualTo(AnalysisProgress.SETUP_PERCENT);
        assertThat(progress.of(analysis, List.of(List.of(0.0, 300.0))).percent()).isEqualTo(54);
        assertThat(progress.of(analysis, List.of(List.of(0.0, 600.0))).percent()).isEqualTo(99);
    }
}
