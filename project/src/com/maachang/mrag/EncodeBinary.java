package com.maachang.mrag;

/**
 * バイナリエンコード.
 */
public class EncodeBinary {
    private EncodeBinary() {}

    public static final byte[] getInt1(int src) {
        return new byte[]{ (byte)(src & 0x0ff) };
    }
    public static final byte[] getInt2(int src) {
        return new byte[] {
            (byte)(src & 0x0ff),
            (byte)((src & 0x0ff00) >> 8)
        };
    }
    public static final byte[] getInt3(int src) {
        return new byte[] {
            (byte)(src & 0x0ff),
            (byte)((src & 0x0ff00) >> 8),
            (byte)((src & 0x0ff0000) >> 16)
        };
    }
    public static final byte[] getInt(int src) {
        return new byte[] {
            (byte)(src & 0x0ff),
            (byte)((src & 0x0ff00) >> 8),
            (byte)((src & 0x0ff0000) >> 16),
            (byte)(((src & 0xff000000) >> 24) & 0x0ff)
        };
    }
    public static final byte[] getFloat(float src) {
        return getInt(Float.floatToRawIntBits(src));
    }
    public static final byte[] getLong(long src) {
        return new byte[] {
            (byte)(src &  0x0ffL),
            (byte)((src & 0x0ff00L) >> 8L),
            (byte)((src & 0x0ff0000L) >> 16L),
            (byte)((src & 0x0ff000000L) >> 24L),
            (byte)((src & 0x0ff00000000L) >> 32L),
            (byte)((src & 0x0ff0000000000L) >> 40L),
            (byte)((src & 0x0ff000000000000L) >> 48L),
            (byte)(((src & 0xff00000000000000L) >> 56L) & 0x0ffL)
        };
    }
    public static final byte[] getDouble(double src) {
        return getLong(Double.doubleToRawLongBits(src));
    }
    public static final byte[] getString(String src) {
        try {
            return src.getBytes("UTF8");
        } catch(Exception e) {
            throw new MRagException(e);
        }
    }

}