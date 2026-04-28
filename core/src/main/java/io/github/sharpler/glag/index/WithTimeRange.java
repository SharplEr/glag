package io.github.sharpler.glag.index;

/// Value that has start and finish timestamps in seconds.
public interface WithTimeRange {

    /// Returns the range start in seconds.
    ///
    /// @return range start in seconds
    double startTimeSec();

    /// Returns the range finish in seconds.
    ///
    /// @return range finish in seconds
    double finishTimeSec();
}
