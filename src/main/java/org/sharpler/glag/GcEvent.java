package org.sharpler.glag;

public record GcEvent(String origin, double timestampSec, int gcNum) {
}
