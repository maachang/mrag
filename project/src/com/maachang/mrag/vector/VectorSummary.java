package com.maachang.mrag.vector;

import java.util.*;

/**
 * llama.cpp で要約された文書を管理する.
 * この情報はVectorGroup単位で管理されます.
 */
public class VectorSummary {
    // Vectorサマリー要素.
    protected static final class VSummaryValue {
        public String text;
        public String url;
        public long time;
        private VSummaryValue() {}
        // コンストラクタ.
        // text: サマリーテキストを設定します.
        // url: 元の情報を示すURLを設定します.
        public VSummaryValue(String text, String url) {
            this(text, url, System.currentTimeMillis());
        }
        // コンストラクタ.
        // text: サマリーテキストを設定します.
        // url: 元の情報を示すURLを設定します.
        // time: サマリー登録時間(UnixTime)を設定します.
        public VSummaryValue(String text, String url, long time) {
            this.text = text;
            this.url = url;
            this.time = time;
        }
    }

    // 要約文書内容をファイル単位で保持する.
    private Map<String, VSummaryValue> summaryList = null;

    // コンストラクタ.
    protected VectorSummary() {
        summaryList = new HashMap<String, VSummaryValue>();
    }

    // コンストラクタ.
    protected VectorSummary(Map<String, VSummaryValue> list) {
        summaryList = list;
    }

    // サマリーリストを取得.
    protected Map<String, VSummaryValue> getList() {
        return summaryList;
    }

    // サマリーを追加.
    // name: 文書名を設定します.
    // vv: サマリー要素を設定します.
    protected void put(String name, VSummaryValue vv) {
        summaryList.put(name, vv);
    }

    // サマリー要素取得.
    // name: 文書名を設定します.
    // 戻り値: サマリー要素が返却されます.
    protected VSummaryValue get(String name) {
        return summaryList.get(name);
    }

    // サマリー文字列を取得.
    // name: 文書名を設定します.
    // 戻り値: サマリー文字列が返却されます.
    public String getText(String name) {
        VSummaryValue vv = summaryList.get(name);
        if(vv == null) {
            return null;
        }
        return vv.text;
    }

    // 元文書のURLを取得.
    // name: 文書名を設定します.
    // 戻り値: 元文書のURLが返却されます.
    public String getUrl(String name) {
        VSummaryValue vv = summaryList.get(name);
        if(vv == null) {
            return null;
        }
        return vv.url;
    }

    // 対象サムネイル登録時間を取得.
    // name: 文書名を設定します.
    // 戻り値: 対象サムネイル登録時間が返却されます.
    public Long getTime(String name) {
        VSummaryValue vv = summaryList.get(name);
        if(vv == null) {
            return null;
        }
        return vv.time;
    }

    // 格納数を取得.
    // 戻り値: 格納数が返却されます.
    public int size() {
        return summaryList.size();
    }

    // 文書名一覧を返却.
    public String[] getDocuments() {
        int i = 0;
        String[] ret = new String[summaryList.size()];
        Iterator<String> itr = summaryList.keySet().iterator();
        while(itr.hasNext()) {
            ret[i ++] = itr.next();
        }
        return ret;
    }
}
