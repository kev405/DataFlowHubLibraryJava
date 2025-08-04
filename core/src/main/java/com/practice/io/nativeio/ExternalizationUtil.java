package com.practice.io.nativeio;

import java.io.*;

public final class ExternalizationUtil {

    private ExternalizationUtil() { }

    public static byte[] toBytes(Externalizable obj) throws IOException {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ObjectOutputStream oos   = new ObjectOutputStream(baos)) {
            obj.writeExternal(oos);
            return baos.toByteArray();
        }
    }

    public static <T extends Externalizable> T fromBytes(byte[] bytes, T empty) throws IOException, ClassNotFoundException {
        try (ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
             ObjectInputStream ois    = new ObjectInputStream(bais)) {
            empty.readExternal(ois);
            return empty;
        }
    }
}
