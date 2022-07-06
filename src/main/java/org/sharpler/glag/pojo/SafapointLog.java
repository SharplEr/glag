package org.sharpler.glag.pojo;

import java.util.List;
import java.util.Map;
import org.sharpler.glag.distribution.CumulativeDistributionPoint;

public record SafapointLog(Map<String, List<SafepointEvent>> events, Map<String, List<CumulativeDistributionPoint>> distributions) {

}
