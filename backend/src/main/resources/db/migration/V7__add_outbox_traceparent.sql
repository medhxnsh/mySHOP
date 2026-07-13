-- V7: carry the W3C trace context through the outbox.
--
-- The outbox decouples event delivery from the request that created the event
-- (OutboxRelay publishes later, on a scheduler thread) — which silently breaks
-- distributed tracing: the Kafka consumer would start a fresh trace instead of
-- continuing the HTTP request's. Persisting the traceparent with the event lets
-- the relay restore the original context before publishing, so one traceId
-- spans HTTP handler -> outbox -> relay -> Kafka consumer.

ALTER TABLE outbox_events ADD COLUMN trace_parent VARCHAR(64);
