package com.maachang.mrag;

import java.util.*;

/**
 * ユーティリティ系.
 */
@SuppressWarnings("unchecked")
public final class Util {
    private Util() {}

    // 配列の一番最初の情報をソートするオブジェクト.
    public static final class SortKeyValue implements Comparable<SortKeyValue> {
        public Comparable key;
        public Object value;
        private SortKeyValue() {}
        // コンストラクタ.
        // key: ソート対象のキーを設定します.
        // value: value情報を設定します.
        public SortKeyValue(Comparable key, Object value) {
            this.key = key;
            this.value = value;
        }
        // 比較処理.
        public int compareTo(SortKeyValue o) {
            return key.compareTo(o.key);
        }
    }

    // SortKeyValueリストを生成.
    // 戻り値: SortKeyValueリストが返却されます.
    public static final List<SortKeyValue> createSortKeyValueList() {
        return new ArrayList<SortKeyValue>();
    }

    // keyValueソート情報を作成.
    // list: 追加対象のListオブジェクトを設定します.
    // key: ソート対象のキーを設定します.
    // value: ソートキーに紐づくValueを設定します.
    public static final void addSortKeyValue(
        List<SortKeyValue> list, Comparable key, Object value) {
        SortKeyValue kv = new SortKeyValue(key, value);
        list.add(kv);
    }

    // 照準ソート処理.
    // list: ソート対象の情報を設定します.
    // 戻り値: ソート結果の情報が返却されます.
    public static final SortKeyValue[] sortKeyValues(List<SortKeyValue> list) {
        return sortKeyValues(list, true);
    }

    // ソート処理.
    // list: ソート対象の情報を設定します.
    // asc: trueの場合照準ソートです.
    // 戻り値: ソート結果の情報が返却されます.
    public static final SortKeyValue[] sortKeyValues(List<SortKeyValue> list, boolean asc) {
        SortKeyValue[] ret;
        SortKeyValue[] sortList;
        int len = list.size();
        ret = new SortKeyValue[len];
        for(int i = 0; i < len; i ++) {
            ret[i] = list.get(i);
        }
        Arrays.sort(ret);
        // 降順ソートの場合.
        if(!asc) {
            // 反転させる.
            sortList = new SortKeyValue[len];
            int n = 0;
            for(int i = len - 1; i >= 0; i --) {
                sortList[n ++] = ret[i];
            }
            ret = sortList;
        }
        return ret;
    }
}