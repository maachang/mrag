package com.maachang.mrag;

import java.util.*;

/**
 * Objectの変換系処理.
 */
@SuppressWarnings("unchecked")
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

    // float情報として取得.
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

    // Mapを作成.
    public static final Map newMap(Object... args) {
        Map map = new HashMap();
        int len = args.length;
        for(int i = 0; i < len; i += 2) {
            map.put(args[i], args[i + 1]);
        }
        return map;
    }

    // Listを作成.
    public static final List newList(Object... args) {
        List list = new ArrayList();
        int len = args.length;
        for(int i = 0; i < len; i ++) {
            list.add(args[i]);
        }
        return list;
    }

    // ファイルから拡張子を除外して取得.
    // fileName: 対象のファイル名を設定します.
    // 戻り値: 拡張子を覗いた文書名が返却されます.
    public static final String getCutExtension(String fileName) {
        int p = fileName.lastIndexOf(".");
        if(p != -1) {
            return fileName.substring(0, p);
        }
        return fileName;
    }

    // マークダウン表記内容を削除する.
    // text: 対象のテキストを設定します.
    // 戻り値: 全角スペースが省かれた内容が返却されます.
    public static String stripMarkdown(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        String result = text;
        // 1. コードブロック (```...```) の除去、または中身だけ残す
        //result = result.replaceAll("(?s)```.*?```", ""); // 中身も除去.
        //result = result.replaceAll("(?s)```(?:[a-z]*\\n)?(.*?)\\n?```", "$1"); // 中身を残す.
        result = result.replaceAll("```(.+?)```", "~~~$1~~~"); // ``` ... ``` を ~~~ ... ~~~ に変換.

        // 2. インラインコード (`code`)
        result = result.replaceAll("`(.+?)`", "$1");
        // 3. 太字・斜体 (***bold***, **bold**, *italic*, __bold__, _italic_)
        result = result.replaceAll("(\\*\\*\\*|___)(.*?)\\1", "$2");
        result = result.replaceAll("(\\*\\*|__)(.*?)\\1", "$2");
        result = result.replaceAll("(\\*|_)(.*?)\\1", "$2");
        // 4. 画像 (![alt](url)) -> alt テキストのみ残す
        result = result.replaceAll("!\\[(.*?)\\]\\(.*?\\)", "$1");
        // 5. リンク ([text](url)) -> text のみ残す
        result = result.replaceAll("\\[(.*?)\\]\\(.*?\\)", "$1");
        // 6. 見出し (# Header) -> 行頭の#を除去
        result = result.replaceAll("(?m)^#{1,6}\\s+", "");
        // 7. 引用 (> Quote) -> 行頭の>を除去
        result = result.replaceAll("(?m)^>\\s+", "");
        // 8. 水平線 (---, ***, ___)
        result = result.replaceAll("(?m)^[\\*\\-_]{3,}\\s*$", "");
        // 9. リストのドットや数字 (1. Item, * Item)
        // これは残す.
        //result = result.replaceAll("(?m)^[\\s\\t]*([\\*\\+-]|\\d+\\.)\\s+", "");
        return result.trim();
    }

    // 不要な本文文字列の除去.
    // text: 対象のテキストを設定します.
    // 戻り値: 不要な文字列が省かれた内容が返却されます.
    public static final String exclusionText(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        char c;
        int len = text.length();
        StringBuilder ret = new StringBuilder(len);
        for(int i = 0; i < len; i ++) {
            c = text.charAt(i);
            if(c == '　' || c == '\r'|| c == '\t') {
                continue;
            }
            ret.append(c);
        }
        return ret.toString();
    }

    // 余分な改行を削除する.
    // text: 対象のテキストを設定します.
    // 戻り値: 不要な文字列が省かれた内容が返却されます.
    public static final String trimEnterText(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        String n;
        String[] list = text.split("\n");
        int len = list.length;
        StringBuilder buf = new StringBuilder(text.length());
        boolean first = true;
        for(int i = 0; i < len; i ++) {
            n = list[i].trim();
            if(n.length() == 0) {
                continue;
            }
            if(!first) {
                buf.append("\n");
            }
            buf.append(n);
            first = false;
        }
        return buf.toString();
    }
}