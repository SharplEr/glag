package org.sharpler.glag.index;

import org.sharpler.glag.util.TimeUtils;

record Range(double start, double finish) {
    boolean match(ValueWithRange<?> value) {
        return TimeUtils.match(start, finish, value.start(), value.finish());
    }
}
