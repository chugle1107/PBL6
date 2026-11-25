-- What the GPU worker is doing before it reports analysed video: downloading (with percent), cutting or loading.
-- Lets the progress bar move during the download and warm-up instead of sitting at 0%.
ALTER TABLE ai_analyses
    ADD COLUMN worker_stage VARCHAR(20),
    ADD COLUMN worker_stage_percent SMALLINT,
    ADD CONSTRAINT chk_ai_analyses_worker_stage_percent CHECK (
        worker_stage_percent IS NULL OR worker_stage_percent BETWEEN 0 AND 100
    );
