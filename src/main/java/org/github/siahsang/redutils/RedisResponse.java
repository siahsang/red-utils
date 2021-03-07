package org.github.siahsang.redutils;

import java.util.Objects;

/**
 * @author Javad Alimohammadi<bs.alimohammadi@gmail.com>
 */

enum RedisResponse {
    /**
     * Ok response from redis
     */
    OK("OK"),
    /**
     * NIL response from redis
     */
    NOT_OK("NOT_OK");

    final String val;

    RedisResponse(String val) {
        this.val = val;
    }

    public static boolean isOk(final String response) {
        return OK.val.equalsIgnoreCase(response);
    }

    public static boolean isNotOK(Object response) {
        return Objects.equals(RedisResponse.NOT_OK.val, response);
    }


}
