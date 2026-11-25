package com.badminton.service.analysis;

import com.badminton.entity.analysis.AiAnalysis;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Progress shown for an analysis. A Mock Engine run reports its own steps; the GPU worker streams results, so
 * its progress is the share of the video already covered.
 */
@Component
@RequiredArgsConstructor
public class AnalysisProgress {

    private final MockAiEngineService mockAiEngineService;

    public record Progress(int percent, String stage) {
    }

    /** analyzedRanges: the parsed analyzed_ranges column, or null. */
    public Progress of(AiAnalysis analysis, List<List<Double>> analyzedRanges) {
        String status = analysis.getStatus();
        if ("COMPLETED".equalsIgnoreCase(status)) {
            return new Progress(100, "completed: Phân tích hoàn tất");
        }
        if ("FAILED".equalsIgnoreCase(status) || "UNSUPPORTED".equalsIgnoreCase(status)) {
            return new Progress(0, "failed: Phiên phân tích gặp sự cố");
        }
        if ("CANCELLED".equalsIgnoreCase(status)) {
            return new Progress(0, "cancelled: Đã hủy");
        }

        int mockPercent = mockAiEngineService.getProgress(analysis.getId());
        if (mockPercent > 0) {
            return new Progress(mockPercent, mockAiEngineService.getCurrentStage(analysis.getId()));
        }
        if (!"PROCESSING".equalsIgnoreCase(status)) {
            return new Progress(0, "Đang chờ trong hàng đợi");
        }
        if (analysis.getAnalyzedUntilSeconds() != null && analysis.getVideoDurationSeconds() != null
                && analysis.getVideoDurationSeconds().signum() > 0) {
            // Several ranges once the viewer has made the worker jump ahead.
            double duration = analysis.getVideoDurationSeconds().doubleValue();
            double covered = analyzedRanges != null
                    ? analyzedRanges.stream().mapToDouble(r -> Math.max(0, r.get(1) - r.get(0))).sum()
                    : analysis.getAnalyzedUntilSeconds().doubleValue();
            double share = Math.min(1, covered / duration);
            return new Progress((int) Math.min(99, SETUP_PERCENT + Math.floor(share * (99 - SETUP_PERCENT))),
                    "streaming: Đang phân tích, kết quả hiện dần trên Replay");
        }
        return setup(analysis.getWorkerStage(), analysis.getWorkerStagePercent());
    }

    /**
     * The first SETUP_PERCENT of the bar is the worker's set-up, so it moves while the video downloads and the
     * models load instead of sitting at 0%. Downloading takes most of that time (a long YouTube match is ~1 GB).
     */
    static final int SETUP_PERCENT = 10;
    private static final int DOWNLOAD_PERCENT = 8;

    static Progress setup(String stage, Short downloadPercent) {
        if (stage == null) {
            return new Progress(0, "starting: Máy phân tích đã nhận việc, đang chuẩn bị");
        }
        return switch (stage) {
            case "downloading" -> {
                int p = downloadPercent == null ? 0 : Math.max(0, Math.min(100, downloadPercent));
                yield new Progress(p * DOWNLOAD_PERCENT / 100,
                        downloadPercent == null ? "downloading: Đang tải video" : "downloading: Đang tải video (" + p + "%)");
            }
            case "cutting" -> new Progress(DOWNLOAD_PERCENT + 1, "cutting: Đang cắt đoạn cần phân tích");
            default -> new Progress(SETUP_PERCENT, "loading: Đang nạp model AI và nhận diện sân");
        };
    }
}
