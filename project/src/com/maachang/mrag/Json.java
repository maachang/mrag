package com.maachang.mrag;

import java.util.*;
import java.lang.reflect.*;

/**
 * Json処理.
 */
public final class Json {
    private static final int TYPE_ARRAY = 0;
    private static final int TYPE_MAP = 1;

    private Json() {}

    // ================================================================
    // Encode
    // ================================================================

    public static String encode(Object target) {
        StringBuilder buf = new StringBuilder(256);
        encodeValue(buf, target, target);
        return buf.toString();
    }

    public static String encode(StringBuilder buf, Object target) {
        encodeValue(buf, target, target);
        return buf.toString();
    }

    private static void encodeValue(StringBuilder buf, Object base, Object target) {
        if (target == null) {
            buf.append("null");
        } else if (target instanceof String) {
            encodeString(buf, (String) target);
        } else if (target instanceof Map<?, ?>) {
            encodeMap(buf, base, (Map<?, ?>) target);
        } else if (target instanceof List<?>) {
            encodeList(buf, base, (List<?>) target);
        } else if (target instanceof Number || target instanceof Boolean) {
            buf.append(target);
        } else if (target instanceof java.util.Date) {
            encodeString(buf, target.toString());
        } else if (target.getClass().isArray()) {
            encodeArray(buf, base, target);
        } else {
            encodeString(buf, target.toString());
        }
    }

    /**
     * JSON文字列エスケープ.
     * RFC 8259準拠: ", \, 制御文字(U+0000〜U+001F)をエスケープ.
     */
    private static void encodeString(StringBuilder buf, String s) {
        buf.append('"');
        int len = s.length();
        for (int i = 0; i < len; i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"':  buf.append("\\\""); break;
                case '\\': buf.append("\\\\"); break;
                case '\b': buf.append("\\b");  break;
                case '\f': buf.append("\\f");  break;
                case '\n': buf.append("\\n");  break;
                case '\r': buf.append("\\r");  break;
                case '\t': buf.append("\\t");  break;
                default:
                    if (c < 0x20) {
                        buf.append("\\u");
                        buf.append(String.format("%04x", (int) c));
                    } else {
                        buf.append(c);
                    }
            }
        }
        buf.append('"');
    }

    private static void encodeMap(StringBuilder buf, Object base, Map<?, ?> map) {
        buf.append('{');
        boolean first = true;
        for (Map.Entry<?, ?> e : map.entrySet()) {
            Object val = e.getValue();
            if (val == base) continue; // 循環参照防止
            if (first) first = false; else buf.append(',');
            encodeString(buf, String.valueOf(e.getKey()));
            buf.append(':');
            encodeValue(buf, base, val);
        }
        buf.append('}');
    }

    private static void encodeList(StringBuilder buf, Object base, List<?> list) {
        buf.append('[');
        int len = list.size();
        boolean first = true;
        for (int i = 0; i < len; i++) {
            Object val = list.get(i);
            if (val == base) continue;
            if (first) first = false; else buf.append(',');
            encodeValue(buf, base, val);
        }
        buf.append(']');
    }

    private static void encodeArray(StringBuilder buf, Object base, Object arr) {
        buf.append('[');
        int len = Array.getLength(arr);
        boolean first = true;
        for (int i = 0; i < len; i++) {
            Object val = Array.get(arr, i);
            if (val == base) continue;
            if (first) first = false; else buf.append(',');
            encodeValue(buf, base, val);
        }
        buf.append(']');
    }

    // ================================================================
    // Decode
    // ================================================================

    public static Object decode(String json) {
        return decode(false, false, json);
    }

    public static Object decode(boolean cutComment, String json) {
        return decode(cutComment, false, json);
    }

    public static Object decode(boolean cutComment, boolean h2Comment, String json) {
        if (json == null) return null;
        if (cutComment) {
            json = stripComments(h2Comment, json);
            if (json == null || json.isEmpty()) return null;
        }
        // 先頭の括弧()を除去
        json = json.trim();
        while (json.startsWith("(") && json.endsWith(")")) {
            json = json.substring(1, json.length() - 1).trim();
        }
        if (json.isEmpty()) return null;

        char first = json.charAt(0);
        if (first == '[' || first == '{') {
            // 構造化JSONをパース
            return new JsonParser(json).parse();
        }
        // スカラー値
        return parseScalar(json);
    }

    // ================================================================
    // Decode: パーサー (インデックスベース, トークンList不要)
    // ================================================================

    /**
     * ストリーム風パーサー.
     * 旧実装のようにトークンListを作成せず、
     * 文字列を直接走査してパースする.
     */
    private static final class JsonParser {
        private final String src;
        private final int len;
        private int pos;

        JsonParser(String src) {
            this.src = src;
            this.len = src.length();
            this.pos = 0;
        }

        /** エントリポイント. */
        Object parse() {
            skipWhitespace();
            Object result = readValue();
            return result;
        }

        /** 任意のJSON値を読む. */
        private Object readValue() {
            skipWhitespace();
            if (pos >= len) return null;
            char c = src.charAt(pos);
            switch (c) {
                case '{': return readMap();
                case '[': return readArray();
                case '"': case '\'': return readString(c);
                default: return readLiteral();
            }
        }

        /** JSON Object (Map) を読む. */
        private Map<String, Object> readMap() {
            pos++; // skip '{'
            Map<String, Object> map = new HashMap<>();
            skipWhitespace();
            if (pos < len && src.charAt(pos) == '}') {
                pos++;
                return map;
            }
            while (pos < len) {
                skipWhitespace();
                // キー読み取り
                String key;
                char c = src.charAt(pos);
                if (c == '"' || c == '\'') {
                    key = readString(c);
                } else {
                    // クォーテーションなしキー対応
                    key = readUnquotedKey();
                }
                skipWhitespace();
                expect(':');
                skipWhitespace();
                Object val = readValue();
                map.put(key, val);
                skipWhitespace();
                if (pos >= len) break;
                c = src.charAt(pos);
                if (c == '}') { pos++; return map; }
                if (c == ',') { pos++; continue; }
                // 寛容: カンマなしでも次の要素へ
            }
            return map;
        }

        /** JSON Array (List) を読む. */
        private List<Object> readArray() {
            pos++; // skip '['
            List<Object> list = new ArrayList<>();
            skipWhitespace();
            if (pos < len && src.charAt(pos) == ']') {
                pos++;
                return list;
            }
            while (pos < len) {
                skipWhitespace();
                list.add(readValue());
                skipWhitespace();
                if (pos >= len) break;
                char c = src.charAt(pos);
                if (c == ']') { pos++; return list; }
                if (c == ',') { pos++; continue; }
            }
            return list;
        }

        /** クォーテーション付き文字列を読む (エスケープ対応). */
        private String readString(char quote) {
            pos++; // skip opening quote
            StringBuilder sb = new StringBuilder();
            while (pos < len) {
                char c = src.charAt(pos);
                if (c == '\\' && pos + 1 < len) {
                    pos++;
                    char esc = src.charAt(pos);
                    switch (esc) {
                        case '"':  sb.append('"');  break;
                        case '\'': sb.append('\''); break;
                        case '\\': sb.append('\\'); break;
                        case '/':  sb.append('/');  break;
                        case 'b':  sb.append('\b'); break;
                        case 'f':  sb.append('\f'); break;
                        case 'n':  sb.append('\n'); break;
                        case 'r':  sb.append('\r'); break;
                        case 't':  sb.append('\t'); break;
                        case 'u':
                            if (pos + 4 < len) {
                                String hex = src.substring(pos + 1, pos + 5);
                                sb.append((char) Integer.parseInt(hex, 16));
                                pos += 4;
                            }
                            break;
                        default:
                            sb.append('\\').append(esc);
                    }
                } else if (c == quote) {
                    pos++;
                    return sb.toString();
                } else {
                    sb.append(c);
                }
                pos++;
            }
            return sb.toString();
        }

        /** クォーテーションなしのキー名を読む. */
        private String readUnquotedKey() {
            int start = pos;
            while (pos < len) {
                char c = src.charAt(pos);
                if (c == ':' || c == ' ' || c == '\t' || c == '\n' || c == '\r') break;
                pos++;
            }
            return src.substring(start, pos).trim();
        }

        /**
         * リテラル値を読む (null, true, false, 数値, クォーテーションなし文字列).
         */
        private Object readLiteral() {
            int start = pos;
            while (pos < len) {
                char c = src.charAt(pos);
                if (c == ',' || c == '}' || c == ']' || c == ':' ||
                    c == ' ' || c == '\t' || c == '\n' || c == '\r') break;
                pos++;
            }
            String token = src.substring(start, pos).trim();
            if (token.isEmpty()) return null;
            return parseScalar(token);
        }

        private void skipWhitespace() {
            while (pos < len) {
                char c = src.charAt(pos);
                if (c == ' ' || c == '\t' || c == '\n' || c == '\r') {
                    pos++;
                } else {
                    break;
                }
            }
        }

        private void expect(char expected) {
            if (pos < len && src.charAt(pos) == expected) {
                pos++;
            } else {
                throw new MRagException(
                    "Expected '" + expected + "' at position " + pos +
                    " but found '" + (pos < len ? src.charAt(pos) : "EOF") + "'");
            }
        }
    }

    // ================================================================
    // スカラー値パース (共通)
    // ================================================================

    /**
     * 文字列をnull/boolean/数値/文字列に変換.
     * 数値判定は1パスで行い、重複パースを避ける.
     */
    private static Object parseScalar(String s) {
        if (s == null || s.isEmpty()) return s;

        // クォーテーション付き文字列
        int len = s.length();
        if (len >= 2) {
            char f = s.charAt(0), l = s.charAt(len - 1);
            if ((f == '"' && l == '"') || (f == '\'' && l == '\'')) {
                return s.substring(1, len - 1);
            }
        }

        // null
        if ("null".equalsIgnoreCase(s)) return null;

        // boolean
        if ("true".equalsIgnoreCase(s)) return Boolean.TRUE;
        if ("false".equalsIgnoreCase(s)) return Boolean.FALSE;

        // 数値 (1パスで整数/小数を判定)
        return parseNumber(s);
    }

    /**
     * 数値パース. パース不可なら元の文字列を返す.
     * Long範囲に収まる整数はLong, 小数点を含む場合はDoubleを返す.
     */
    private static Object parseNumber(String s) {
        boolean hasDecimal = false;
        int start = 0;
        int len = s.length();
        if (len == 0) return s;

        char c0 = s.charAt(0);
        if (c0 == '-' || c0 == '+') {
            if (len == 1) return s; // "-" or "+" alone
            start = 1;
        }

        for (int i = start; i < len; i++) {
            char c = s.charAt(i);
            if (c == '.') {
                if (hasDecimal) return s; // 小数点2つはNG
                hasDecimal = true;
            } else if (c == 'e' || c == 'E') {
                // 指数表記 -> Doubleにフォールバック
                hasDecimal = true;
            } else if (c == '+' || c == '-') {
                // 指数部の符号は許可 (前がe/E)
                if (i == 0 || (s.charAt(i - 1) != 'e' && s.charAt(i - 1) != 'E')) {
                    return s;
                }
            } else if (c < '0' || c > '9') {
                return s; // 数値でない
            }
        }

        try {
            if (hasDecimal) {
                return Double.parseDouble(s);
            } else {
                // まずLongで試行、溢れたらDoubleにフォールバック
                try {
                    return Long.parseLong(s);
                } catch (NumberFormatException e) {
                    return Double.parseDouble(s);
                }
            }
        } catch (NumberFormatException e) {
            return s;
        }
    }

    // ================================================================
    // コメント除去
    // ================================================================

    /**
     * コメント除去.
     * 対応: //, /* ... *​/, #, (オプション) --
     */
    private static String stripComments(boolean h2, String str) {
        if (str == null || str.isEmpty()) return "";
        int len = str.length();
        StringBuilder buf = new StringBuilder(len);
        int quote = -1;      // -1: クォーテーション外
        int comment = -1;    // -1: コメント外, 1: 行コメント, 2: ブロックコメント

        for (int i = 0; i < len; i++) {
            char c = str.charAt(i);

            // --- コメント内 ---
            if (comment != -1) {
                if (comment == 1) {
                    // 行コメント: 改行で終了
                    if (c == '\n') {
                        buf.append(c);
                        comment = -1;
                    }
                } else {
                    // ブロックコメント: */ で終了
                    if (c == '\n') {
                        buf.append(c);
                    } else if (c == '*' && i + 1 < len && str.charAt(i + 1) == '/') {
                        i++;
                        comment = -1;
                    }
                }
                continue;
            }

            // --- クォーテーション内 ---
            if (quote != -1) {
                buf.append(c);
                if (c == (char) quote && !isEscaped(str, i)) {
                    quote = -1;
                }
                continue;
            }

            // --- 通常文字 ---
            // クォーテーション開始
            if ((c == '"' || c == '\'') && !isEscaped(str, i)) {
                quote = c;
                buf.append(c);
                continue;
            }

            // //  or  /* ... */
            if (c == '/' && i + 1 < len) {
                char n = str.charAt(i + 1);
                if (n == '/') { comment = 1; continue; }
                if (n == '*') { comment = 2; i++; continue; }
            }

            // -- (h2コメント)
            if (h2 && c == '-' && i + 1 < len && str.charAt(i + 1) == '-') {
                comment = 1; continue;
            }

            // #
            if (c == '#') { comment = 1; continue; }

            buf.append(c);
        }
        return buf.toString();
    }

    /**
     * 位置iの文字がバックスラッシュでエスケープされているか判定.
     */
    private static boolean isEscaped(String s, int i) {
        int count = 0;
        for (int j = i - 1; j >= 0; j--) {
            if (s.charAt(j) == '\\') count++;
            else break;
        }
        return (count & 1) == 1;
    }
}