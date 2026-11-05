local key = KEYS[1]
local blocked_key = KEYS[2]
local request_id = ARGV[1]
local window_size = tonumber(ARGV[2])
local lock_ttl = tonumber(ARGV[3])
local max_attempt = tonumber(ARGV[4])

local t = redis.call("TIME");
local now = t[1] * 1000 + math.floor(t[2]/1000)

if redis.call("EXISTS", blocked_key) == 1 then
    return 0
end

redis.call("ZREMRANGEBYSCORE", key, "-inf", now - window_size)
redis.call("ZADD", key, now, request_id)
redis.call("PEXPIRE", key, window_size)

if redis.call("ZCARD", key) > max_attempt then
    redis.call("UNLINK", key)
    redis.call("SET", blocked_key, "blocked", "NX", "PX", lock_ttl)
    return 0
end

return 1