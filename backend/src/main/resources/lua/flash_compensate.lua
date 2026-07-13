-- Compensation for a failed reservation hand-off (Phase 9).
-- If the Kafka publish fails AFTER the purchase script accepted the buyer,
-- the reservation would be lost — so the stock unit and the buyer's slot are
-- returned atomically, letting them retry.
--
-- KEYS[1] = flash:{saleId}:stock
-- KEYS[2] = flash:{saleId}:buyers
-- ARGV[1] = userId

redis.call('INCR', KEYS[1])
redis.call('SREM', KEYS[2], ARGV[1])
return 1
