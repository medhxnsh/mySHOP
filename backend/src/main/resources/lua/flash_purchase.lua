-- Atomic flash-sale purchase (Phase 9).
--
-- WHY LUA? Redis executes a script as one atomic unit — the check-active,
-- already-bought, stock-check and decrement below cannot interleave with
-- another buyer's execution. Doing this as separate commands from Java
-- would reintroduce the exact check-then-act race that causes overselling.
--
-- KEYS[1] = flash:{saleId}:stock   (counter, pre-warmed at activation)
-- KEYS[2] = flash:{saleId}:buyers  (set of userIds)
-- ARGV[1] = userId
--
-- Returns:  >= 0  remaining stock after this purchase (accepted)
--           -1    sold out
--           -2    sale not active (stock key absent)
--           -3    this user already bought

if redis.call('EXISTS', KEYS[1]) == 0 then
  return -2
end
if redis.call('SISMEMBER', KEYS[2], ARGV[1]) == 1 then
  return -3
end
local stock = tonumber(redis.call('GET', KEYS[1]))
if stock <= 0 then
  return -1
end
redis.call('DECR', KEYS[1])
redis.call('SADD', KEYS[2], ARGV[1])
return stock - 1
