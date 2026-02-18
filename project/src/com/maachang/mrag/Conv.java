package com.maachang.mrag;

import java.util.*;

/**
 * 変換系処理.
 */
public final class Conv {
    private Conv() {}

    // boolean情報として取得.
    public static final boolean getBoolean(Object o) {
        if(o instanceof Boolean) {
            return (Boolean)o;
        } else if(o instanceof String) {
            String s = ((String)o).trim().toLowerCase();
            if(s == "true") {
                return true;
            } else {
                return false;
            }
        }
        throw new MRagException("Boolean conversion failed: " + o);
    }

    // int情報として取得.
    public static final int getInt(Object o) {
        if(o instanceof Integer) {
            return (Integer)o;
        } else if(o instanceof Number) {
            return ((Number)o).intValue();
        } else if(o instanceof String) {
            try {
                return Integer.parseInt(((String)o).trim());
            } catch(Exception e) {}
        }
        throw new MRagException("Integer conversion failed: " + o);
    }

    // long情報として取得.
    public static final long getLong(Object o) {
        if(o instanceof Long) {
            return (Long)o;
        } else if(o instanceof Number) {
            return ((Number)o).longValue();
        } else if(o instanceof String) {
            try {
                return Long.parseLong(((String)o).trim());
            } catch(Exception e) {}
        }
        throw new MRagException("Long conversion failed: " + o);
    }

    // double情報として取得.
    public static final float getFloat(Object o) {
        if(o instanceof Float) {
            return (Float)o;
        } else if(o instanceof Number) {
            return ((Number)o).floatValue();
        } else if(o instanceof String) {
            try {
                return Float.parseFloat(((String)o).trim());
            } catch(Exception e) {}
        }
        throw new MRagException("Float conversion failed: " + o);
    }

    // double情報として取得.
    public static final double getDouble(Object o) {
        if(o instanceof Double) {
            return (Double)o;
        } else if(o instanceof Number) {
            return ((Number)o).doubleValue();
        } else if(o instanceof String) {
            try {
                return Double.parseDouble(((String)o).trim());
            } catch(Exception e) {}
        }
        throw new MRagException("Double conversion failed: " + o);
    }

    // string情報として取得.
    public static final String getString(Object o) {
        if(o == null) {
            throw new MRagException("String conversion failed: " + o);
        } else if(o instanceof String) {
            return (String)o;
        }
        return o.toString();
    }

    // map情報として取得.
    public static final Map getMap(Object o) {
        if(o instanceof Map) {
            return (Map)o;
        }
        throw new MRagException("Map conversion failed: " + o);
    }

    // list情報として取得.
    public static final List getList(Object o) {
        if(o instanceof List) {
            return (List)o;
        }
        throw new MRagException("List conversion failed: " + o);
    }
}