package com.practice.bench;

import org.openjdk.jmh.annotations.*;

import com.practice.io.JsonSerializer;
import com.practice.io.nativeio.DataFileExternalizable;
import com.practice.io.nativeio.ExternalizationUtil;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.AverageTime) @OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Thread)
public class ExternalizationBench {

    private DataFileExternalizable[] array;
    private String[] jsons;

    @Setup
    public void setup() {
        array = new DataFileExternalizable[10_000];
        jsons = new String[array.length];
        for (int i = 0; i < array.length; i++) {
            array[i] = new DataFileExternalizable(
                    UUID.randomUUID(), "/tmp/file" + i, "abc" + i);
            jsons[i] = JsonSerializer.toJson(array[i]);
        }
    }

    /* ---------- tamaño en bytes -------------------------------------- */

    @Benchmark public int size_externalizable() throws Exception {
        int sum = 0;
        for (var df : array) sum += ExternalizationUtil.toBytes(df).length;
        return sum;
    }
    @Benchmark public int size_json() {
        int sum = 0;
        for (String s : jsons) sum += s.getBytes().length;
        return sum;
    }

    /* ---------- tiempo de serializar --------------------------------- */

    @Benchmark public int serialize_externalizable() throws Exception {
        int last = 0;
        for (var df : array) last = ExternalizationUtil.toBytes(df).length;
        return last;
    }
    @Benchmark public int serialize_json() {
        int last = 0;
        for (var df : array) last = JsonSerializer.toJson(df).length();
        return last;
    }
}
