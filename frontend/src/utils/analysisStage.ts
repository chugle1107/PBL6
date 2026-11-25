import type { AnalysisStatus } from '../types/replay';

/**
 * currentStage is "code: text" (see backend AnalysisProgress). The code says where a run is:
 * starting/downloading/cutting/loading while the worker sets up, streaming once video is analysed.
 * The mock engine reports its own codes (hit_scan, …), which count as analysing.
 */
export const stageCode = (status?: AnalysisStatus): string =>
  (status?.currentStage ?? '').match(/^([a-z_]+):/i)?.[1]?.toLowerCase() ?? '';

/** "streaming: Đang phân tích..." -> "Đang phân tích...". */
export const stageText = (status?: AnalysisStatus): string => {
  if (!status) return 'Đang kết nối máy phân tích…';
  const raw = status.currentStage || '';
  const text = raw.replace(/^[a-z_]+:\s*/i, '').trim();
  if (!text || /^[a-z_]+$/i.test(text)) return status.status === 'QUEUED' ? 'Đang chờ trong hàng đợi' : 'Đang phân tích';
  return text;
};

export const ANALYSIS_PHASES = ['Hàng đợi', 'Tải video', 'Nạp AI', 'Phân tích', 'Hoàn tất'] as const;

/** Index into ANALYSIS_PHASES. Works for the mock engine and the GPU worker alike. */
export const analysisPhase = (status?: AnalysisStatus): number => {
  if (!status || status.status === 'QUEUED' || status.status === 'NONE') return 0;
  if (status.status === 'COMPLETED') return 4;
  const code = stageCode(status);
  if (code === 'starting' || code === 'downloading' || code === 'cutting') return 1;
  if (code === 'loading') return 2;
  return 3;
};
