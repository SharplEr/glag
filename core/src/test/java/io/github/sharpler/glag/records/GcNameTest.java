package io.github.sharpler.glag.records;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class GcNameTest {
    @Test
    void findGcName() {
        Assertions.assertEquals(
            GcName.G1,
            GcName.findGcName("[187770.595s] G1 Service Thread (Periodic GC Task) (run: 0.030ms) (cpu: 0.000ms)")
        );
    }
}
