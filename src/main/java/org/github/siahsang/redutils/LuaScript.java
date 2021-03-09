package org.github.siahsang.redutils;

/**
 * @author Javad Alimohammadi<bs.alimohammadi@gmail.com>
 */

public interface LuaScript {
    // @formatter:off
     String GET_LOCK = "if redis.call('EXISTS', KEYS[1]) == 0 then " +
                       "    redis.call('SET', KEYS[1], ARGV[1], 'NX', 'PX', ARGV[2]) " +
                       "    return 'OK'" +
                       "elseif redis.call('GET', KEYS[1]) == ARGV[1] then " +
                       "    redis.call('EXPIRE', KEYS[1], ARGV[2]) " +
                       "    return 'OK' " +
                       "else " +
                       "    return 'NOT_OK' "+
                       "end ";




    String RELEASE_LOCK = "if redis.call('get',KEYS[1]) == ARGV[1] then " +
                          "    return redis.call('del', KEYS[1]) " +
                          "else " +
                          "    return 'NOT_OK' " +
                          "end";
}
