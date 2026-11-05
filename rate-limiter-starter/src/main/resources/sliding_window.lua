local key = KEYS[1]
local request_id = ARGV[1]
local window_size = tonumber(ARGV[2])
local max_attempt = tonumber(ARGV[3])

local t = redis.call("TIME");
local now = t[1] * 1000 + math.floor(t[2]/1000)

redis.call("ZREMRANGEBYSCORE", key, "-inf", now - window_size)

if redis.call("ZCARD", key) >= max_attempt then
    return 0
end

redis.call("ZADD", key, now, request_id)
redis.call("PEXPIRE", key, window_size)
return 1