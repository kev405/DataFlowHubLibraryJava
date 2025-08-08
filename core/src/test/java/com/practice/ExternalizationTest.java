package com.practice;

import org.junit.jupiter.api.Test;

import com.practice.io.nativeio.DataFileExternalizable;
import com.practice.io.nativeio.ExternalizationUtil;

import java.io.IOException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ExternalizationTest {

    @Test
    void roundTrip_equals() throws IOException, ClassNotFoundException {
        DataFileExternalizable df =
            new DataFileExternalizable(UUID.randomUUID(), "/tmp/a", "xyz");
        byte[] bytes = ExternalizationUtil.toBytes(df);

        DataFileExternalizable copy =
            ExternalizationUtil.fromBytes(bytes, new DataFileExternalizable());
        assertEquals(df.id(),        copy.id());
        assertEquals(df.storagePath(), copy.storagePath());
        assertEquals(df.checksum(),  copy.checksum());
    }
}
