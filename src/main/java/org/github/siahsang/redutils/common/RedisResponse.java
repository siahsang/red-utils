package org.github.siahsang.redutils.common;

import java.util.Objects;

/**
 * @author Javad Alimohammadi<bs.alimohammadi@gmail.com>
 */

public enum RedisResponse {
    /**
     * Ok response from redis
     */
    SUCCESS("SUCCESS"),
    /**
     * NIL response from redis
     */
    FAIL("FAIL");

    final String val;

    RedisResponse(String val) {
        this.val = val;
    }

    public static boolean isSuccessFull(final String response) {
        return SUCCESS.val.equalsIgnoreCase(response);
    }

    public static boolean isFailed(Object response) {
        return Objects.equals(RedisResponse.FAIL.val, response);
    }


}
