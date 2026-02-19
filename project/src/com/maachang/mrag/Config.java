package com.maachang.mrag;

import java.util.*;

/**
 * 実行起動コンフィグ情報.
 */
@SuppressWarnings("unchecked")
public class Config {
    // LLAMAサーバステータス.
    public static final class  LlamaCppState {
        public final String baseUrl;
        private boolean helth;
        private long updateTime = -1L;
        private LlamaCppState() {
            baseUrl = null;
        }
        // コンストラクタ.
        public LlamaCppState(String url) {
            baseUrl = url;
            update();
        }
        // サーバ利用可能かアップデート.
        public boolean update() {
            // 更新タイミングのみチェック.
            if(updateTime == -1L ||
                updateTime + HEALTH_CHECK_TIME < System.currentTimeMillis()) {
                return check();
            }
            return helth;
        }

        // 直接チェック.
        public boolean check() {
            helth = LlamaCpp.health(baseUrl);
            updateTime = System.currentTimeMillis();
            return helth;
        }
    }

    // JSON情報.
    private Map json;

    // [llama.cpp]接続確認タイミング(15秒に１度)
    private static long HEALTH_CHECK_TIME = 15000L;

    // [llama.cpp]組み込みサーバ接続先.
    private final List<LlamaCppState> embeddingList = new ArrayList<LlamaCppState>();

    // [llama.cpp]チャットサーバ接続先.
    private final List<LlamaCppState> chatList = new ArrayList<LlamaCppState>();

    // チャンク単位の文字列長.
    private int chunkSize;

    // 次のチャンクに設定する文字列長.
    private int overlapSize;

    


    


}