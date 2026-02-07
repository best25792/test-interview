-- Add traceparent column to event_outbox table (stores full W3C Trace Context traceparent header)
-- Format: 00-{traceId}-{spanId}-{flags} (max length ~55 characters)
ALTER TABLE event_outbox ADD COLUMN IF NOT EXISTS traceparent VARCHAR(100);

-- Create index on traceparent for faster lookups
CREATE INDEX IF NOT EXISTS idx_outbox_traceparent ON event_outbox(traceparent);
