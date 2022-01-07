package rs.cache;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import rs.util.Util;
import rs.util.NetworkBuffer;
import rs.util.FastIntMap;

public class Archive {
    public int id;
    public int hash;
    public int crc;
    public int version;
    private final Int2ObjectMap<Group> groups = new FastIntMap<>();
    private final Int2ObjectMap<Group> namedGroups = new Int2ObjectOpenHashMap<>();

    public Archive(int id) {
        this.id = id;
    }

    public void load(int indexCrc, int indexVersion) {
        crc = indexCrc;
        version = indexVersion;

        byte[] data = CacheFiles.read(255, id);

        if (data == null || Util.crc(data) != indexCrc && indexCrc != 0) {
            requestMaster();
            return;
        }

        NetworkBuffer decompressed = new NetworkBuffer(Util.decompress(data));
        int type = decompressed.readUnsignedByte();

        if (type != 5 && type != 6) {
            throw new IllegalStateException();
        }

        if (type == 6) {
            decompressed.readInt();
        }

        loadIndex(data);
    }

    public void requestMaster() {
        Js5.request(255, id, true, data -> {
            if (Util.crc(data) != crc) {
                throw new AssertionError("crc");
            }

            CacheFiles.write(255, id, data);
            loadIndex(data);
        });
    }

    private void loadIndex(byte[] data) {
        hash = Util.hash(data);
        NetworkBuffer buffer = new NetworkBuffer(Util.decompress(data));
        int type = buffer.readUnsignedByte();

        if (type != 5 && type != 6) {
            throw new RuntimeException();
        }

        if (type == 6) {
            buffer.readInt();
        }

        boolean hasNames = buffer.readUnsignedByte() != 0;
        int gc = type >= 7 ? buffer.readShortOrInt() : buffer.readUnsignedShort();

        Group[] groups = new Group[gc];

        for (int i = 0; i < groups.length; i++) {
            groups[i] = new Group();
        }

        int id = 0;

        for (Group group : groups) {
            id += buffer.readUnsignedShort();
            group.id = id;
            this.groups.put(id, group);
        }

        if (hasNames) {
            for (Group group : groups) {
                namedGroups.put(buffer.readInt(), group);
            }
        }

        for (Group group : groups) group.crc = buffer.readInt();
        for (Group group : groups) group.version = buffer.readInt();
        for (Group group : groups) group.fileCount = buffer.readUnsignedShort();

        for (Group group : groups) {
            group.fileIds = new int[group.fileCount];
            int fileId = 0;

            for (int i = 0; i < group.fileCount; ++i) {
                fileId += buffer.readUnsignedShort();
                group.fileIds[i] = fileId;
            }
        }

        if (hasNames) {
            for (Group group : groups) {
                group.fileNameHashes = new int[group.fileCount];

                for (int i = 0; i < group.fileCount; ++i) {
                    group.fileNameHashes[group.fileNameHashes[i]] = buffer.readInt();
                }
            }
        }

        for (Group group : this.groups.values()) {
            byte[] groupData = CacheFiles.read(this.id, group.id);

            if (groupData == null || Util.crc(groupData) != group(group.id).crc) {
                Js5.request(this.id, group.id, false, d -> {
                    if (Util.crc(d) != group(group.id).crc) {
                        throw new IllegalStateException("received crc doesn't match");
                    }

                    CacheFiles.write(this.id, group.id, d);
                    group(group.id).data = d;
                });
            } else {
                group(group.id).data = groupData;
            }
        }
    }

    public Group group(int group) {
        return groups.get(group);
    }

    public Group group(String name) {
        return namedGroups.get(Util.hash(name.toLowerCase()));
    }
}
