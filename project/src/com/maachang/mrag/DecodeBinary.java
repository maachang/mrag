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
        return (int)(binary[position ++]);
    }
    public int getUInt1() {
        return (int)((binary[position ++]) & 0x0ff);
    }
    public int getInt2() {
        return (int)
            (binary[position ++]) |
            ((binary[position ++]) << 8);
    }
    public int getUInt2() {
        return (int)
            (((binary[position ++]) |
            ((binary[position ++]) << 8)) & 0x0ffff);
    }
    public int getInt3() {
        return (int)
            (binary[position ++]) |
            ((binary[position ++]) << 8) |
            ((binary[position ++]) << 16);
    }
    public int getUInt3() {
        return (int)
            (((binary[position ++]) |
            ((binary[position ++]) << 8) |
            ((binary[position ++]) << 16)) & 0x0ffffff);
    }
    public int getInt() {
        return (int)
            (binary[position ++]) |
            ((binary[position ++]) << 8) |
            ((binary[position ++]) << 16) |
            ((binary[position ++]) << 24);
    }
    public float getFloat() {
        return Float.intBitsToFloat(getInt());
    }
    public long getLong() {
        return (long)
            (binary[position ++]) |
            ((binary[position ++]) << 8L) |
            ((binary[position ++]) << 16L) |
            ((binary[position ++]) << 24L) |
            ((binary[position ++]) << 32L) |
            ((binary[position ++]) << 40L) |
            ((binary[position ++]) << 48L) |
            ((binary[position ++]) << 56L);
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