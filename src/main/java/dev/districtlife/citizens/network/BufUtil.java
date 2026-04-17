package dev.districtlife.citizens.network;

import io.netty.buffer.ByteBuf;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * Utilitaires de lecture/écriture sur ByteBuf utilisant UNIQUEMENT les méthodes
 * Netty (jamais remappées par Forge/SRG).
 *
 * DLCitizens est un plugin Bukkit — il n'est PAS remappé par FML.
 * Les méthodes Minecraft comme readString/writeString/readUUID/writeUUID/readVarInt
 * ont des noms SRG à runtime et ne sont donc PAS accessibles directement.
 * Cette classe implémente les mêmes encodages (VarInt + UTF-8, long pair pour UUID)
 * en n'utilisant que readByte/readBytes/writeByte/writeBytes/readLong/writeLong.
 */
public final class BufUtil {

    private BufUtil() {}

    // ── VarInt ────────────────────────────────────────────────────────────────

    public static int readVarInt(ByteBuf buf) {
        int value = 0;
        int shift = 0;
        byte b;
        do {
            b = buf.readByte();
            value |= (b & 0x7F) << shift;
            shift += 7;
            if (shift > 35) throw new RuntimeException("VarInt trop grand");
        } while ((b & 0x80) != 0);
        return value;
    }

    public static void writeVarInt(ByteBuf buf, int value) {
        while ((value & ~0x7F) != 0) {
            buf.writeByte((value & 0x7F) | 0x80);
            value >>>= 7;
        }
        buf.writeByte(value);
    }

    // ── String (VarInt longueur en octets + UTF-8) ────────────────────────────

    public static String readString(ByteBuf buf, int maxLength) {
        int byteLen = readVarInt(buf);
        if (byteLen > maxLength * 4) {
            throw new IllegalArgumentException("String trop longue : " + byteLen + " octets");
        }
        byte[] bytes = new byte[byteLen];
        buf.readBytes(bytes);
        String s = new String(bytes, StandardCharsets.UTF_8);
        if (s.length() > maxLength) {
            throw new IllegalArgumentException("String trop longue : " + s.length() + " chars");
        }
        return s;
    }

    public static void writeString(ByteBuf buf, String s, int maxLength) {
        if (s.length() > maxLength) {
            throw new IllegalArgumentException("String trop longue : " + s.length() + " chars");
        }
        byte[] bytes = s.getBytes(StandardCharsets.UTF_8);
        writeVarInt(buf, bytes.length);
        buf.writeBytes(bytes);
    }

    // ── UUID (deux long : mostSignificantBits, leastSignificantBits) ──────────

    public static UUID readUUID(ByteBuf buf) {
        long msb = buf.readLong();
        long lsb = buf.readLong();
        return new UUID(msb, lsb);
    }

    public static void writeUUID(ByteBuf buf, UUID uuid) {
        buf.writeLong(uuid.getMostSignificantBits());
        buf.writeLong(uuid.getLeastSignificantBits());
    }
}
