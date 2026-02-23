package com.maachang.mrag.vector;

import java.util.*;
import java.util.concurrent.*;
import java.nio.file.*;

import com.maachang.mrag.*;

/**
 * １つのベクトルグループ.
 */
public class VectorGroup {
    // ベクトルストアーグループ名.
    private final String group;

    // ファイルパス.
    private final String filePath;

    // ファイル名.
    private final String fileName;

    // ファイル日付.
    private final long fileTime;

    // vectorStore情報.
    private VectorChunk[] documents;

    // vectorSummary情報.
    private VectorSummary summarys;

    // VectorChunkキャッシュ.
    private Queue<VectorChunk> cache;

    // コンストラクタ.
    private VectorGroup() {
        group = null;
        filePath = null;
        fileName = null;
        fileTime = -1L;
    }

    // コンストラクタ.
    // group: ベクトルストアグループ名を設定します.
    // path: ファイルパス名を設定します.
    // name: ファイル名を設定します.
    // docs: ベクトルストア情報を設定します.
    // smms: ベクトルサマリー情報を設定します.
    // cman: VectorChunkキャッシュ管理Queueを設定します.
    public VectorGroup(String group, String path, String name,
        long time, VectorChunk[] docs, VectorSummary smms,
        Queue<VectorChunk> cman) {
        this.group = group;
        this.filePath = path;
        this.fileName = name;
        this.fileTime = time;
        this.documents = docs;
        this.summarys = smms;
        this.cache = cman == null ?
            new LinkedList<VectorChunk>() : cman;
    }

    // キャッシュ情報から VectorChunk を取得.
    private final VectorChunk getCache() {
        VectorChunk ret = cache.poll();
        if(ret == null) {
            ret = new VectorChunk();
        }
        return ret;
    }

    // 得点を計算: コサイン類似度.
    private static final double score(float[] a, float[] b) {
        double d = 0.0d, na = 0.0d, nb = 0.-d;
        float av, bv;
        int i, len = a.length;
        for(i = 0; i < len; i ++) {
            av = a[i]; bv = b[i];
            d += (double)(av * bv);
            na += (double)(av * av);
            nb += (double)(bv * bv);
        }
        return (d / (Math.sqrt(na * nb) + 1.0E-10));
        //return (d / (Math.sqrt(na) * Math.sqrt(nb) + 1e-10));
    }

    // 検索結果を返却.
    // out: 取得対象の検索結果格納配列を設定します.
    // queryEmbedding: 組み込みモデルで生成された検索ベクトル配列を設定します.
    // 戻り値: out に格納された長さが返却されます.
    public int search(VectorChunk[] out, float[] queryEmbedding) {
        // 単純検索.
        int i;
        final int len = documents.length;
        if(len == 0) {
            return 0;
        }
        // 同じサイズの配列を生成.
        VectorChunk[] target = new VectorChunk[len];
        // 近い言葉の座標を計算する.
        for(i = 0; i < len; i ++) {
            target[i] = getCache();
            documents[i].copy(target[i]);
            // 得点計算.
            target[i].score = score(queryEmbedding, target[i].embedding);
        }
        // ソート処理で得点の高い順にソート.
        Arrays.sort(target);
        // 取得結果を返却する.
        final int outLen = out.length;
        int ret = 0;
        VectorChunk n;
        for(i = 0; i < outLen; i ++) {
            if(i >= len) {
                // documents側の情報の方がoutLenより少ない場合.
                break;
            }
            n = new VectorChunk();
            target[i].copy(n);
            out[i] = n;
            ret ++;
        }
        // キャッシュセット.
        for(i = 0; i < len; i ++) {
            cache.offer(target[i]);
        }
        return ret;
    }

    // グループ内のVectorChunk群を取得.
    // 戻り値: VectorChunk群が返却されます.
    public VectorChunk[] getDocuments() {
        return documents;
    }

    // グループ無いのVectorSummaryを取得.
    // 戻り値: VectorSummaryが返却されます.
    public VectorSummary getSummary() {
        return summarys;
    }

    // グループ名を取得.
    // 戻り値: グループ名が返却されます.
    public String getGroup() {
        return group;
    }

    // vectorGroupのファイル名を取得.
    // 戻り値: ファイル名が返却されます.
    public String getFileName() {
        return fileName;
    }

    // vectorGroupのファイル更新時間を取得.
    // 戻り値: ファイル更新時間が返却されます.
    public long getFileTime() {
        return fileTime;
    }

    // 対象ファイルが更新されたか確認.
    // 戻り値: true が返却された場合はファイルが更新されています.
    public boolean isUpdateFile() {
        try {
            Path p = Paths.get(filePath + "/" + fileName);
            if(fileTime != Files.getLastModifiedTime(p).to(TimeUnit.MILLISECONDS)) {
                return true;
            }
            return false;
        } catch(Exception e) {
            throw new MRagException(e);
        }
    }

    // グループに登録されているファイル名群を取得.
    // 戻り値: 現状のVectorGroupに格納されているファイル群が返却されます.
    public String[] getFileNames() {
        int i;
        Set<String> out = new HashSet<String>();
        int len = documents.length;
        for(i = 0; i < len; i ++) {
            out.add(documents[i].fileName);
        }
        len = out.size();
        String[] ret = new String[len];
        Iterator<String> it = out.iterator();
        i = 0;
        while(it.hasNext()) {
            ret[i ++] = it.next();
        }
        return ret;
    }
}