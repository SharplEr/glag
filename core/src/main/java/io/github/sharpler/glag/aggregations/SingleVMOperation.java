package io.github.sharpler.glag.aggregations;

import io.github.sharpler.glag.records.SafepointLogRecord;

/// A slow safepoint that was not matched to any GC iteration.
///
/// @param safepointLog the unmatched safepoint event
public record SingleVMOperation(SafepointLogRecord safepointLog) {
}
