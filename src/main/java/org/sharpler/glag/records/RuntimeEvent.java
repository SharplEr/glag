package org.sharpler.glag.records;

import java.util.List;

public sealed interface RuntimeEvent {
    record SingleVMOperation(SafepointLogRecord safepointLog) implements RuntimeEvent {
    }

    record GcIteration(List<SafepointLogRecord> safepointLog, List<GcLogRecord> gcLog) implements RuntimeEvent {
        public int gcNum() {
            return gcLog.get(0).gcNum();
        }
    }
}
