package io.github.sharpler.glag.distribution;

/// One point of a cumulative distribution.
///
/// @param value metric value at this point
/// @param prob cumulative probability in the range `[0, 1]`
public record CumulativeDistributionPoint(long value, double prob) {
}
