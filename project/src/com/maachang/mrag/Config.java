package com.maachang.mrag;

import java.util.*;

/**
 * 実行起動コンフィグ情報.
 */
@SuppressWarnings("unchecked")
public final class Config {
    // LLAMAサーバ情報.
    public static final class  LlamaCppInfo {
        // 基本URL(http://ip-addr:port).
        public final String baseUrl;
        // タイプ: 0: 推論モード, 1: 組み込みモード, -1: 不明
        public final int llamaType;
        // ヘルスチェック状態(llama.cppが稼働している場合 true)
        public boolean helth;
        // ヘルスチェック最終時間.
        public long lastHelthTime;
        
        // コンストラクタ.
        private LlamaCppInfo() {
            baseUrl = null;
            llamaType = -1;
        }
        // コンストラクタ.
        public LlamaCppInfo(String url, int type) {
            baseUrl = url;
            llamaType = type;
            lastHelthTime = -1L;
            helth = false;
            update();
        }
        // サーバ利用可能な場合、アップデート.
        public boolean update() {
            long chkTime = Config.SNGL.healthCheckTiming;
            // 更新タイミングのみチェック.
            if(lastHelthTime == -1L ||
                lastHelthTime + chkTime < System.currentTimeMillis()) {
                return check();
            }
            return helth;
        }

        // 直接チェック.
        public boolean check() {
            helth = LlamaCpp.health(baseUrl);
            lastHelthTime = System.currentTimeMillis();
            return helth;
        }
    }

    // 1つのLlamaCppInfoを生成.
    private static final LlamaCppInfo getLlamaCppInfo(
        Object map, int type, int no) {
        String url = Conv.getString(mapToGetValue(map, "url", "")).trim();
        if(url.length() == 0) {
            throw new MRagException(
                "The URL for llamaCpp connection destination (type: " + type +
                ", no: " + no + ") is not set.");
        }
        return new LlamaCppInfo(url, type);
    }

    // 指定キー名を設定して LlamaCppInfo群を取得.
    private static final List<LlamaCppInfo> getLlamaCppInfoList(
        List<LlamaCppInfo> out, Object map, String name, int type) {
        Object info = Conv.getMap(map).get(name);
        if(info == null) {
            // nameの条件が存在しない場合.
            throw new MRagException(
                "llamaCpp destination: " + name +
                " definition does not exist.");
        }
        // 要素が１つの場合.
        if(info instanceof Map) {
            out.clear();
            out.add(getLlamaCppInfo(info, type, 0));
            return out;
        }
        // 要素が複数の場合.
        else if(info instanceof List) {
            out.clear();
            List list = Conv.getList(info);
            int len = list.size();
            for(int i = 0; i < len; i ++) {
                out.add(getLlamaCppInfo(list.get(i), type, i));
            }
            return out;
        }
        // 定義型が条件に合わないのでエラー.
        throw new MRagException(
            "llamaCpp destination: " + name +
            " Invalid definition: " + info.getClass().getName());
    }

    // map情報のキー名が存在する場合はその値を返却.
    private static final Object mapToGetValue(
        Object map, String name, Object defValue) {
        Object ret = Conv.getMap(map).get(name);
        if(ret == null) {
            return defValue;
        }
        return ret;
    }

    // コンストラクタ.
    private Config() {};
    // シングルトン.
    private static final Config SNGL = new Config();

    // Configオブジェクトが返却されます.
    public static final Config getInstance() {
        return SNGL;
    }

    ///////////////////////////////////////////
    // HTTPサーバ設定関連.
    ///////////////////////////////////////////

    ///////////////////////////////////////////
    // ファイルパス関連.
    ///////////////////////////////////////////

    // vectorStore格納先パス.
    public String vectorStorePath = Const.DEFAULT_VECTOR_STORE_PATH;

    // 参照ファイル格納先パス.
    public String srcDocumentPath = Const.DEFAULT_SRC_DOCUMENT_PATH;

    ///////////////////////////////////////////
    // llama.cpp管理.
    ///////////////////////////////////////////

    // 利用可能なLlamaCppの接続URLを取得.
    private static final String getLlamaCppBaseUrl(
        int type, List<LlamaCppInfo> list, AInt seqNo) {
        int no;
        LlamaCppInfo info;
        int max = list.size();
        // 生きているllama.cppにアクセス.
        for(int i = 0; i < max; i ++) {
            no = seqNo.get();
            if(no + 1 >= max) {
                seqNo.set(0);
            } else {
                seqNo.set(no + 1);
            }
            info = list.get(no);
            if(info.helth) {
                return info.baseUrl;
            }
        }
        // すべてが死んでるので、再度アクセス可能か直接接続して確認.
        for(int i = 0; i < max; i ++) {
            no = seqNo.get();
            if(no + 1 >= max) {
                seqNo.set(0);
            } else {
                seqNo.set(no + 1);
            }
            info = list.get(no);
            if(info.check()) {
                return info.baseUrl;
            }
        }
        // すべて接続不可の場合.
        if(type == Const.LLAMA_CPP_TYPE_CHAT) {
            // 推論モード.
            throw new MRagException(
                "Failed to get connection URL for llama.cpp in inference mode.");
        } else {
            // 組み込みモード.
            throw new MRagException(
                "Failed to get connection URL for llama.cpp in embedded mode.");
        }
    }

    // [llama.cpp]組み込みサーバ接続先.
    private final List<LlamaCppInfo> embeddingList = new ArrayList<LlamaCppInfo>();
    private final AInt embeddingSeqNo = new AInt();
    public String getEmbeddingURL() {
        // 組み込みモードの基本URLを取得.
        return getLlamaCppBaseUrl(
            Const.LLAMA_CPP_TYPE_EMBEDDING, embeddingList, embeddingSeqNo);
    }

    // [llama.cpp]チャットサーバ接続先.
    private final List<LlamaCppInfo> chatList = new ArrayList<LlamaCppInfo>();
    private final AInt chatListSeqNo = new AInt();
    public String getChatURL() {
        // 推論モードの基本URLを取得.
        return getLlamaCppBaseUrl(
            Const.LLAMA_CPP_TYPE_CHAT, embeddingList, embeddingSeqNo);
    }

    // llama動作確認タイミング.
    public long healthCheckTiming = Const.DEFAULT_HEALTH_CHECK_TIMING;

    ///////////////////////////////////////////
    // 参照ドキュメントに対するchunk定義.
    ///////////////////////////////////////////

    // チャンク単位の文字列長.
    public int chunkSize = Const.DEFAULT_JP_CHANK_SIZE;

    // 次のチャンクに設定する文字列長.
    public int overlapSize = Const.chunkSizeToOverlapSize(Const.DEFAULT_JP_CHANK_SIZE);

    ///////////////////////////////////////////
    // サマリー関連.
    ///////////////////////////////////////////

    // サマリー作成Temperatureパラメータ.
    public float summaryTemperature = Const.DEFAULT_SUMMARY_TEMPERATURE;

    // サマリー作成推論モード.
    public String summaryReasoningMode = Const.DEFAULT_REASONING_MODE;

    // サマリー問い合わせフォーマット.
    // ※この情報の取得は専用メソッドで取得する.
    private String summaryRequestFormat = Const.SUMMARY_REQUEST_FOEMAT;

    // サマリー問い合わせメッセージを取得.
    // text: サマリー変換対象のtextを設定します.
    // 戻り値: サマリー問い合わせメッセージが返却されます.
    public String getSummaryRequest(String text) {
        return Conv.keyValueTemplate(summaryRequestFormat,
            "reasoningMode", summaryReasoningMode, "text", text);
    }

    ///////////////////////////////////////////
    // RAGリクエスト関連.
    ///////////////////////////////////////////

    // Ragの問い合わせに対するベクトル計算結果に対する検索数.
    public int vectorSearchLength = Const.DEFAULT_VECTOR_SEARCH_LENGTH;

    // Ragの問い合わせに対するチャンク設定件数.
    public int ragRequestChunkLength = Const.DEFAULT_RAG_REQUEST_CHANK_LENGTH;

    // RagTemperatureパラメータ.
    public float ragTemperature = Const.DEFAULT_RAG_TEMPERATURE;

    // Rag作成推論モード.
    public String ragReasoningMode = Const.DEFAULT_REASONING_MODE;

    // Ragの問い合わせに対する、1つのチャンク設定を定義するフォーマット.
    // ※この情報の取得は専用メソッドで取得する.
    private String ragRequestChunkFormat = Const.DEFAULT_RAG_REQUEST_CHUNK_FORMAT;

    // Ragの問い合わせに対する、1つのチャンクメッセージを取得.
    // no: 対象の１から始まる番号を設定します.
    // name: 対象の文書名を設定します.
    // url: 対象の文書URLを設定します.
    // score: 対象のスコアを設定します.
    // text: 対象の文書名のサマリーテキストを設定します.
    // 戻り値: Ragのチャンク問い合わせメッセージが返却されます
    public String getRagRequestChunk(
        int no, String name, String url, float score, String summary) {
        return Conv.keyValueTemplate(ragRequestChunkFormat,
            "no", no, "name", name, "url", url, "score", score,
            "summary", summary); 
    }

    // Ragの問い合わせフォーマット.
    // ※この情報の取得は専用メソッドで取得する.
    private String ragRequestFormat = Const.DEFAULT_RAG_REQUEST_FORAMT;

    // Ragの問い合わせメッセージを取得.
    // chunkMessages: getRagRequestChunkで作成された複数のチャンクメッセージ群を設定します.
    // message Rag質問メッセージを設定します.
    // 戻り値: Ragの問い合わせメッセージが返却されます.
    public String getRagRequest(String chunkMessages, String message) {
        return Conv.keyValueTemplate(ragRequestChunkFormat,
            "reasoningMode", ragReasoningMode,
            "chunkMessages", chunkMessages, "message", message); 
    }

    // コンフィグロード.
    public void loadConfig() {
        loadConfig(Const.DEFAULT_CONFIG_PATH, Const.DEFAULT_CONFIG_FILE);
    }

    // コンフィグロード.
    // path: コンフィグファイルパスを設定します.
    // fileName: コンフィグファイル名を設定します.
    public void loadConfig(String path, String fileName) {
        // コンフィグファイルをロードして、文字列をJSON変換.
        Object json = Json.decode(
            Util.readFileToString(path, fileName));
        // コンフィグ内容を反映.
        setConfig(json);
    }

    // コンフィグ内容を読み込む.
    // json: 対象のJSONオブジェクトを設定します.
    protected void setConfig(Object json) {

        // [llama.cpp管理定義]組み込みサーバ接続先.
        getLlamaCppInfoList(embeddingList, json, "embeddingList", Const.LLAMA_CPP_TYPE_EMBEDDING);
        // [llama.cpp管理定義]推論サーバ接続先.
        getLlamaCppInfoList(chatList, json, "chatList", Const.LLAMA_CPP_TYPE_CHAT);
        // [llama.cpp管理定義]llama動作確認タイミング.
        healthCheckTiming = Conv.getLong(mapToGetValue(json, "healthCheckTiming", healthCheckTiming));

        // [ファイルパス定義]vectorStore格納先パス.
        vectorStorePath = Conv.getString(mapToGetValue(json, "vectorStorePath", vectorStorePath));
        // [ファイルパス定義]参照ファイル格納先パス.
        srcDocumentPath = Conv.getString(mapToGetValue(json, "srcDocumentPath", srcDocumentPath));

        // [チャンク定義]チャンク単位の文字列長.
        chunkSize = Conv.getInt(mapToGetValue(json, "chunkSize", chunkSize));
        // [チャンク定義]次のチャンクに設定する文字列長.
        overlapSize = Conv.getInt(mapToGetValue(json, "overlapSize", overlapSize));

        // [サマリー定義]サマリー作成Temperatureパラメータ.
        summaryTemperature = Conv.getFloat(mapToGetValue(json, "summaryTemperature", summaryTemperature));
        // [サマリー定義]サマリー作成推論モード.
        summaryReasoningMode = Conv.getString(mapToGetValue(json, "summaryReasoningMode", summaryReasoningMode));
        // [サマリー定義]サマリー問い合わせフォーマット.
        summaryRequestFormat = Conv.getString(mapToGetValue(json, "summaryRequestFormat", summaryRequestFormat));

        // [Rag定義]Ragの問い合わせに対するベクトル計算結果に対する検索数.
        vectorSearchLength = Conv.getInt(mapToGetValue(json, "vectorSearchLength", vectorSearchLength));
        // [Rag定義]Ragの問い合わせに対するチャンク設定件数.
        ragRequestChunkLength = Conv.getInt(mapToGetValue(json, "ragRequestChunkLength", ragRequestChunkLength));
        // [Rag定義]RagTemperatureパラメータ.
        ragTemperature = Conv.getFloat(mapToGetValue(json, "ragTemperature", ragTemperature));
        // [Rag定義]Rag作成推論モード.
        ragReasoningMode = Conv.getString(mapToGetValue(json, "ragReasoningMode", ragReasoningMode));
        // [Rag定義]Ragの問い合わせに対する、1つのチャンク設定を定義するフォーマット.
        ragRequestChunkFormat = Conv.getString(mapToGetValue(json, "ragRequestChunkFormat", ragRequestChunkFormat));
        // [Rag定義]Ragの問い合わせフォーマット.
        ragRequestFormat = Conv.getString(mapToGetValue(json, "ragRequestFormat", ragRequestFormat));

    }

    
}