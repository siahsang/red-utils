package org.github.siahsang.test.redis;

public enum AOFConfiguration {
    ALWAYS("always"), EVERY_SECOND("everysec"), NO("no");

    public String value;

    AOFConfiguration(String value) {
        this.value = value;
    }


}
