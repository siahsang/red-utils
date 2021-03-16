package org.github.siahsang.redutils.common.redis;

/**
 * @author Javad Alimohammadi<bs.alimohammadi@gmail.com>
 */

public interface LuaScript {
    // @formatter:off
     String GET_LOCK = String.format(
                       "if redis.call('EXISTS', KEYS[1]) == 0 then " +
                       "    redis.call('SET', KEYS[1], ARGV[1], 'NX', 'PX', ARGV[2]) " +
                       "    return '%s'" +
                       "elseif redis.call('GET', KEYS[1]) == ARGV[1] then " +
                       "    redis.call('EXPIRE', KEYS[1], ARGV[2]) " +
                       "    return '%s' " +
                       "else " +
                       "    return '%s' "+
                       "end ", RedisResponse.SUCCESS, RedisResponse.SUCCESS, RedisResponse.FAIL);




    String RELEASE_LOCK = String.format(
                          "if redis.call('get',KEYS[1]) == ARGV[1] then " +
                          "   redis.call('del', KEYS[1]) " +
                          "   return '%s' " +
                          "else " +
                          "    return '%s' " +
                          "end", RedisResponse.SUCCESS,  RedisResponse.FAIL);
}
