package com.maachang.mrag.vector;

import java.util.*;

/**
 * llama.cpp で要約された文書を管理する.
 * この情報はVectorGroup単位で管理されます.
 */
public class VectorSummary {
    // 要約文書内容をファイル単位で保持する.
    private Map<String, String> summaryList = null;

    // コンストラクタ.
    protected VectorSummary() {
        summaryList = new HashMap<String, String>();
    }

    // コンストラクタ.
    protected VectorSummary(Map<String, String> list) {
        summaryList = list;
    }

    // サマリーリストを取得.
    protected Map<String, String> getList() {
        return summaryList;
    }

    // サマリーを追加.
    protected void put(String name, String value) {
        summaryList.put(name, value);
    }

    // サマリー取得.
    // name: 対象のファイル名を設定します.
    // 戻り値: サマリー情報が返却されます.
    public String get(String name) {
        return summaryList.get(name);
    }

    // 格納数を取得.
    // 戻り値: 格納数が返却されます.
    public int size() {
        return summaryList.size();
    }

    // ファイル名一覧を返却.
    public String[] getNames() {
        int i = 0;
        String[] ret = new String[summaryList.size()];
        Iterator<String> itr = summaryList.keySet().iterator();
        while(itr.hasNext()) {
            ret[i ++] = itr.next();
        }
        return ret;
    }

}
