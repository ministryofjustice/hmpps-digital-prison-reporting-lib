CREATE TABLE missing_report_submission
(
    id BIGSERIAL PRIMARY KEY NOT NULL,
    userId TEXT NOT NULL,
    reportId TEXT NOT NULL,
    reportVariantId text NOT NULL,
    reason TEXT
);