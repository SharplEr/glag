package org.sharpler.glag.index;

import org.sharpler.glag.util.TimeUtils;

public record Range(double start, double finish) {
    public boolean match(ValueWithRange value) {
        return TimeUtils.match(start, finish, value.start(), value.finish());
    }
}
