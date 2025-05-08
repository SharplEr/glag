package org.sharpler.glag.parsing;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.sharpler.glag.records.SafepointLogRecord;

class SafepointParserTest {

    @Test
    void parse() {
        var line =
            "[2024-12-16T07:05:07.368+0000][3.412s][info][safepoint] Safepoint \"ICBufferFull\", Time since last: 177611286 ns, Reaching safepoint: 69282 ns, Cleanup: 130048 ns, At safepoint: 8449 ns, Total: 207779 ns";

        var result = SafepointParser.parse(line, 0);

        Assertions.assertEquals(
            new SafepointLogRecord(
                "2024-12-16T07:05:07.368+0000",
                3.412 - 207779 / 1E9,
                3.412,
                "ICBufferFull",
                69282L,
                8449L,
                207779L,
                0
            ),
            result
        );
    }
}
