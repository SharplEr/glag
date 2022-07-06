package org.sharpler.glag.aggregations;

import java.util.List;
import java.util.Map;
import org.sharpler.glag.distribution.CumulativeDistributionPoint;
import org.sharpler.glag.pojo.SafepointEvent;

public record SafapointLog(Map<String, List<SafepointEvent>> events, Map<String, List<CumulativeDistributionPoint>> distributions) {

}
