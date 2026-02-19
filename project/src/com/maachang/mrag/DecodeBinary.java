package com.maachang.mrag;

/**
 * バイナリデコード.
 */
public class DecodeBinary {
    private int position;
    private byte[] binary;
    private int length;
    
    // コンストラクタ.
    // 
    public DecodeBinary(byte[] binary) {
        this.binary = binary;
        this.length = binary.length;
        this.position = 0;
    }

    public int getInt1() {
        int n = (int)(binary[position ++]);
        return ((n & 0x80) != 0) ? n | 0xffffff00 : n;
    }
    public int getUInt1() {
        return (int)(binary[position ++] & 0x0ff);
    }
    public int getInt2() {
        int n = (int)
            ((binary[position ++] & 0x0ff) |
            ((binary[position ++] & 0x0ff) << 8));
        return ((n & 0x8000) != 0) ? n | 0xffff0000 : n;
    }
    public int getUInt2() {
        return (int)
            (((binary[position ++] & 0x0ff) |
            ((binary[position ++] & 0x0ff) << 8)) & 0x0ffff);
    }
    public int getInt3() {
        int n = (int)
            ((binary[position ++] & 0x0ff) |
            ((binary[position ++] & 0x0ff) << 8) |
            ((binary[position ++] & 0x0ff) << 16));
        return ((n & 0x800000) != 0) ? n | 0xff000000 : n;
    }
    public int getUInt3() {
        return (int)
            (((binary[position ++] & 0x0ff) |
            ((binary[position ++] & 0x0ff) << 8) |
            ((binary[position ++] & 0x0ff) << 16)) & 0x0ffffff);
    }
    public int getInt() {
        return (int)
            ((binary[position ++] & 0x0ff) |
            ((binary[position ++] & 0x0ff) << 8) |
            ((binary[position ++] & 0x0ff) << 16) |
            ((binary[position ++] & 0x0ff) << 24));
    }
    public float getFloat() {
        return Float.intBitsToFloat(getInt());
    }
    public long getLong() {
        return (long)
            ((binary[position ++] & 0x0ffL) |
            ((binary[position ++] & 0x0ffL) << 8L) |
            ((binary[position ++] & 0x0ffL) << 16L) |
            ((binary[position ++] & 0x0ffL) << 24L) |
            ((binary[position ++] & 0x0ffL) << 32L) |
            ((binary[position ++] & 0x0ffL) << 40L) |
            ((binary[position ++] & 0x0ffL) << 48L) |
            ((binary[position ++] & 0x0ffL) << 56L));
    }
    public double getDouble() {
        return Double.longBitsToDouble(getLong());
    }
    public String getString(int len) {
        try {
            String ret = new String(binary, position, len, "UTF8");
            position += len;
            return ret;
        } catch(Exception e) {
            throw new MRagException(e);
        }
    }

    // 現在のポジションを取得.
    public int getPosition() {
        return position;
    }

    // バイナリ長を取得.
    public int getLength() {
        return binary.length;
    }

    // 現在の残りバイナリ長を取得.
    public int getRemaining() {
        return binary.length - position;
    }
}