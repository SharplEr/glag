package org.sharpler.glag.pojo;

import java.util.List;
import java.util.Map;

public sealed interface SafepointExplain {
    record SimpleExplain(String message) implements SafepointExplain {
    }

    final class NoExplain implements SafepointExplain {
        public static final NoExplain INSTANCE = new NoExplain();

        private NoExplain() {
            // No-op.
        }
    }

    record GcExplain(Map<Integer, List<GcEvent>> gcs) implements SafepointExplain {

    }
}
