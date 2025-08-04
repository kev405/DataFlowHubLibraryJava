package com.practice.io.nativeio;

import java.io.*;
import java.util.UUID;

/**
 * Binary, space-efficient representation of DataFile.
 * Only essential fields are written: id, storagePath, checksumSha256.
 */
public class DataFileExternalizable implements Externalizable {

    private UUID   id;
    private String storagePath;
    private String checksumSha256;

    /* REQUIRED no-args ctor for Externalizable */
    public DataFileExternalizable() { }

    public DataFileExternalizable(UUID id, String storagePath, String checksumSha256) {
        this.id = id;
        this.storagePath = storagePath;
        this.checksumSha256 = checksumSha256;
    }

    /* ---------------- getters (equals/hash omitted for brevity) -------- */

    public UUID   id()            { return id; }
    public String storagePath()   { return storagePath; }
    public String checksum()      { return checksumSha256; }

    /* ---------------- Externalizable ----------------------------------- */

    @Override public void writeExternal(ObjectOutput out) throws IOException {
        out.writeLong(id.getMostSignificantBits());
        out.writeLong(id.getLeastSignificantBits());
        out.writeUTF(storagePath);
        out.writeUTF(checksumSha256);
    }

    @Override public void readExternal(ObjectInput in) throws IOException {
        long msb = in.readLong();
        long lsb = in.readLong();
        this.id           = new UUID(msb, lsb);
        this.storagePath  = in.readUTF();
        this.checksumSha256 = in.readUTF();
    }
}
