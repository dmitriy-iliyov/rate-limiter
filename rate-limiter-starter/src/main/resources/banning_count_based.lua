local key = KEYS[1]
local blocked_key = KEYS[2]
local windowSize = tonumber(ARGV[1])
local lock_ttl = tonumber(ARGV[2])
local max_attempt = tonumber(ARGV[3])

if redis.call("EXISTS", blocked_key) == 1 then
    return 0
end

local attempt = redis.call("INCR", key)

if attempt == 1 then
    redis.call("PEXPIRE", key, windowSize)
end

if attempt > max_attempt then
    redis.call("DEL", key)
    redis.call("SET", blocked_key, "blocked", "NX", "PX", lock_ttl)
    return 0
end

return 1