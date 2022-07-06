package org.sharpler.glag.pojo;

import java.util.List;
import java.util.Map;

public record GcLog(Map<Integer, List<GcEvent>> events, List<GcTime> times, double startLogSec, double finishLogSec) {

}
