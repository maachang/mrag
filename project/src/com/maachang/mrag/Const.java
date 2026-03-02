package com.maachang.mrag;

/**
 * 定義条件.
 */
public final class Const {
    // コンストラクタ.
    private Const() {}

    ///////////////////////////////////////////////////////////////////////////
    // LlamaCppタイプ: 0: 推論モード, 1: 組み込みモード.
    ///////////////////////////////////////////////////////////////////////////

    // LlamaCppタイプ: 推論モード.
    public static final int LLAMA_CPP_TYPE_CHAT = 0;

    // LlamaCppタイプ: 組み込みモード.
    public static final int LLAMA_CPP_TYPE_EMBEDDING = 1;

    ///////////////////////////////////////////////////////////////////////////
    // Httpサーバ関連.
    ///////////////////////////////////////////////////////////////////////////

    ///////////////////////////////////////////////////////////////////////////
    // システム関連.
    ///////////////////////////////////////////////////////////////////////////

    // デフォルトのコンフィグパス.
    public static final String DEFAULT_CONFIG_PATH = "./conf";

    // デフォルトのコンフィグファイル名.
    public static final String DEFAULT_CONFIG_FILE = "mrag.json";

    // デフォルトのvectorStore格納先.
    public static final String DEFAULT_VECTOR_STORE_PATH = "./vectorStore";

    // デフォルトの参照ファイル格納先.
    // 実際にこの下に「グループ名のパス」が作成されて、その下に
    // {
    //      text: 参照ファイル文字列,
    //      name: 参照ファイル元の文書名,
    //      url: 参照ファイル元のドキュメントのURL,
    //      time: ファイル生成時間
    // }
    // のJSONファイル名が {{name}}.json として格納される.
    // またこのファイルは、以下の条件で利用される.
    //   - vectorStore登録時に保管される.
    //   - vectorStoreファイルの生成, 追加に利用される.
    public static final String DEFAULT_SRC_DOCUMENT_PATH = "./docs";

    ///////////////////////////////////////////////////////////////////////////
    // RAG関連.
    ///////////////////////////////////////////////////////////////////////////

    // [llama.cpp]接続確認タイミング(15秒に１度)
    public static final long DEFAULT_HEALTH_CHECK_TIMING = 15000L;

    // デフォルトチャンクサイズ(日本語用)
    public static final int DEFAULT_JP_CHANK_SIZE = 300;

    // チャックサイズに対するOverlapサイズの割合.
    public static final float CHUNK_SIZE_TO_OVERLAP_COEFFICIENT = 0.25f;

    // チャンクサイズに対するOverlapサイズの割合計算.
    // chunkSize: 対象のチャンクサイズを設定します.
    // 戻り値: overlapサイズが返却されます.
    public static final int chunkSizeToOverlapSize(int chunkSize) {
        return (int)((float)chunkSize * CHUNK_SIZE_TO_OVERLAP_COEFFICIENT);
    }

    // 推論モード(推論モデルが有効になる).
    public static final String REASONING_MODE_SMALL ="small";
    public static final String REASONING_MODE_MEDIUM = "medium";
    public static final String REASONING_MODE_LARGE = "large";

    // デフォルト推論モード(medium).
    public static final String DEFAULT_REASONING_MODE = REASONING_MODE_MEDIUM;

    // デフォルトの文書サマリ回答に対するTemperatureパラメータ値(0に近いほど正確性)の値.
    //  ・ 0.1 - 0.3: 正確性重視（事実・指示）
    //  ・ 0.7 - 0.8: バランス重視（対話）
    //  ・ 1.0 - 1.2: 創造性重視（物語・創作）
    public static final float DEFAULT_SUMMARY_TEMPERATURE = 0.3f;

    // デフォルトのサマリーリクエストフォーマット.
    public static final String SUMMARY_REQUEST_FOEMAT =
        "<reasoning_mode>{{reasoningMode}}</reasoning_mode>\n" +
        "以下の内容のサマリーを日本語で詳しくまとめて頂きたい。\n\n ---\n{{text}}";

    // デフォルトのRagの問い合わせに対するベクトル計算結果に対する検索数.
    public static final int DEFAULT_VECTOR_SEARCH_LENGTH = 30;

    // デフォルトのRagの問い合わせに対するチャンク設定件数.
    public static final int DEFAULT_RAG_REQUEST_CHANK_LENGTH = 6;

    // デフォルトのRagの問い合わせに対するTemperatureパラメータ値(0に近いほど正確性)の値.
    //  ・ 0.1 - 0.3: 正確性重視（事実・指示）
    //  ・ 0.7 - 0.8: バランス重視（対話）
    //  ・ 1.0 - 1.2: 創造性重視（物語・創作）
    public static final float DEFAULT_RAG_TEMPERATURE = 0.15f;

    // デフォルトのRagの問い合わせに対する、1つのチャンク設定を定義するフォーマット.
    public static final String DEFAULT_RAG_REQUEST_CHUNK_FORMAT =
        "【参考文書番号: {{no}}】(参考文書名:{{name}}, 参考文書URL: {{url}}, 類似度:{{score}})\n{{summary}}\n---\n\n";

    // デフォルトのRagの問い合わせフォーマット.
    public static final String DEFAULT_RAG_REQUEST_FORAMT = """
        <reasoning_mode>{{reasoningMode}}</reasoning_mode>
        あなたは日本語で回答する専門家アシスタントです。
        以下の参考文書を使って、質問者の意見を汲んで、分かりやすく答えてください。
        参考文書の内容に対して該当質問の回答が無く質問に答えられない場合は「情報はありませんでした。」だけを必ず答えてください。
        一方で「情報はありませんでした。」以外の回答の場合は、以下の指示に従ってください。
            - 回答作成で参照した参考文書名を、リスト型で「URLリンクのmarkdown形式」で行い、
            1から始まるリスト番号を「参考文書名」の前に必ず記載してください。
            - 「参考文書」は、回答内容の「一番最後の行に列挙」し、タイトルとして「参照文書一覧」を必ず記載してください。
            - 回答に関する参照文書一覧には、利用・参照しなかった「参考文書名」は列挙しないでください。
            - 回答が「情報はありませんでした。」以外の場合は「情報はありませんでした。」の文字を利用しないでください。
            
        以上の条件で回答してください。
        ---

        {{chunkMessages}}
        質問: {{message}}
        回答:""";



}