package com.maachang.mrag.vector;

/**
 * １つのベクトル塊.
 */
public class VectorChunk implements Comparable<VectorChunk> {
    // １つのファイルのベクトル塊のテキスト文.
    public String text;
    // １つのファイルのベクトルデータ.
    public double[] embedding;
    // ファイル名.
    public String fileName;
    // １つのファイルのベクトル塊項番.
    public int indexNo;
    // １つのファイルのベクトル塊総数.
    public int allLength;

    // スコアー情報.
    public double score;

    // コンストラクタ.
    public VectorChunk() {}

    // コンストラクタ.
    // text: １つのファイルのベクトル塊のテキスト文.
    // no: １つのファイルのベクトル塊項番.
    // allLen: １つのファイルのベクトル塊総数.
    //         nameのファイル内容の１つのテキストを分割した総数がここに入ります.
    // name: ファイル名.
    // emb: １つのファイルのベクトルデータ.
    public VectorChunk(String text, int no, int allLen, String name, double[] emb) {
        this.text = text; 
        this.embedding = emb;
        this.fileName = name;
        this.indexNo = no;
        this.allLength = allLen;
        this.score = -1;
    }

    // コピー処理.
    // out: コピー先オブジェクトが存在する場合設定します.
    // 戻り値: コピーされた VectorChunkオブジェクト が返却されます.
    public VectorChunk copy(VectorChunk out) {
        if(out == null) {
            out = new VectorChunk();
        }
        out.text = this.text;
        out.embedding = this.embedding;
        out.fileName = this.fileName;
        out.indexNo = this.indexNo;
        out.allLength = this.allLength;
        out.score = this.score;
        return out;
    }

    // [降順]ソート条件を返却.
    // Comparable インターフェイス実装用.
    public int compareTo(VectorChunk o) {
        if(o.score < score) {
            return -1;
        } else if(o.score > score) {
            return 1;
        }
        return 0;
    }
}

