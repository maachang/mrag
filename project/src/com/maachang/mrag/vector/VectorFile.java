package com.maachang.mrag.vector;

import java.io.*;
import java.util.*;
import java.nio.file.*;
import java.util.concurrent.*;

import com.maachang.mrag.*;

/**
 * VectorStoreに対する１つのファイルに関する処理.
 */
@SuppressWarnings("unchecked")
public final class VectorFile {
    private VectorFile() {}

    // VectorGroupファイルシンボル.
    private static final String VECTOR_GROUP_FILE_SIMBOL = "@vgs";

    // VectorGroupファイル拡張子.
    public static final String VECTOR_GROUP_FILE_EXTENSION = ".vgs";

    // VectorChunk群をファイルロード処理.
    // fileName: ファイル名を指定してロード処理を行います.
    // 戻り値: VectorChunk[] が返却されます.
    public static final VectorChunk[] load(String fileName) {
        if(!fileName.endsWith(VECTOR_GROUP_FILE_EXTENSION)) {
            // 拡張子が存在しない場合は拡張子をセット.
            fileName += VECTOR_GROUP_FILE_EXTENSION;
        }
        int len;
        byte[] b, buf;
        ByteArrayOutputStream bo = new ByteArrayOutputStream();
        InputStream in = null;
        buf = new byte[8192];
        try {
            in = new FileInputStream(fileName);
            while(true) {
                if((len = in.read(buf)) == -1) {
                    break;
                }
                bo.write(buf, 0, len);
            }
            in.close();
            in = null;
            b = bo.toByteArray();
            bo.close();
            bo = null;
        } catch(Exception e) {
            throw new MRagException(e);
        } finally {
            if(in != null) {
                try { in.close(); } catch(Exception e) {}
            }
        }
        return load(b);
    }

    // VectorChunk群をバイナリロード処理.
    // binary: バイナリを指定してロード処理を行います.
    // 戻り値: VectorChunk[] が返却されます.
    public static final VectorChunk[] load(byte[] binary) {
        DecodeBinary bd = new DecodeBinary(binary);
        // ファイルシンボルの確認.
        String simbol = bd.getString(4);
        if(!VECTOR_GROUP_FILE_SIMBOL.equals(simbol)) {
            throw new MRagException("Not a VectorGroup file symbol");
        }
        // 最初にVectorChunk数を取得.
        int allLen = bd.getUInt3();
        VectorChunk[] ret = new VectorChunk[allLen];
        // binary化されてるVectorChunk群をdeSerialize.
        int i, j, indexNo, len;
        String fileName, text;
        double[] embList;
        for(i = 0; i < allLen; i ++) {
            // インデックスNoを取得.
            indexNo = bd.getUInt3();
            // ファイル名を取得.
            len = bd.getUInt2();
            fileName = bd.getString(len);
            // テキストを取得.
            len = bd.getUInt3();
            text = bd.getString(len);
            // embeddingの長さを取得.
            len = bd.getUInt3();
            embList = new double[len];
            for(j = 0; j < len; j ++) {
                // 1つのembeddingを取得.
                embList[j] = bd.getDouble();
            }
            ret[i] = new VectorChunk(
                text, indexNo, allLen, fileName, embList);
        }
        return ret;
    }

    // VectorChunk群をファイルに保存(serialize)
    // fileName: 保存先のファイル名を設定します.
    // chunks: 保存対象の VectorChunk 群を設定します.
    public static final void save(String fileName, VectorChunk[] chunks) {
        if(!fileName.endsWith(VECTOR_GROUP_FILE_EXTENSION)) {
            // 拡張子が存在しない場合は拡張子をセット.
            fileName += VECTOR_GROUP_FILE_EXTENSION;
        }
        FileOutputStream fo = null;
        BufferedOutputStream bo = null;
        try {
            fo = new FileOutputStream(fileName);
            bo = new BufferedOutputStream(fo);
            save(bo, chunks);
            fo.close();
            fo = null;
            bo.close();
            bo = null;
        } catch(MRagException me) {
            throw me;
        } catch(Exception e) {
            throw new MRagException(e);
        } finally {
            if(fo != null) {
                try { fo.close(); } catch(Exception e) {}
            }
            if(bo != null) {
                try { bo.close(); } catch(Exception e) {}
            }
        }
    }

    // VectorChunk群を保存(serialize)
    // 保存先のOutputStreamを設定します.
    // chunks: 保存対象の VectorChunk 群を設定します.
    public static final void save(OutputStream out, VectorChunk[] chunks) {
        int i, j, len;
        byte[] bin;
        double[] embList;
        VectorChunk ck;
        int allLen = chunks.length;
        try {
            // ファイルシンボルを出力.
            out.write(EncodeBinary.getString(VECTOR_GROUP_FILE_SIMBOL));
            // 最初にVectorChunk数を保存.
            out.write(EncodeBinary.getInt3(allLen));
            // 保存対象のVectorChunk群をループ実行.
            for(i = 0; i < allLen; i ++) {
                ck = chunks[i];
                // インデックスNoを保存.
                out.write(EncodeBinary.getInt3(ck.indexNo));
                // ファイル名を保存.
                bin = EncodeBinary.getString(ck.fileName);
                out.write(EncodeBinary.getInt2(bin.length));
                out.write(bin);
                // テキストを保存.
                bin = EncodeBinary.getString(ck.text);
                out.write(EncodeBinary.getInt3(bin.length));
                out.write(bin);
                // embeddingを保存.
                embList = ck.embedding;
                len = embList.length;
                // embeddingの長さを保存.
                out.write(EncodeBinary.getInt3(len));
                for(j = 0; j < len; j ++) {
                    // 1つのembeddingを保存.
                    out.write(EncodeBinary.getDouble(embList[i]));
                }
            }
            out.flush();
        } catch(Exception e) {
            throw new MRagException(e);
        }
    }

    // テキストをチャンク単位で分割.
    // text: 対象のテキストを設定します.
    // chunkSize: チャンク単位の文字列長を設定します.
    // overlapSize: 次のチャンクに設定する文字列長を設定します.
    // 戻り値: チャンク単位で区切られた文字列が返却されます.
    public static final List<String> stringToChunks(
        String text, int chunkSize, int overlapSize) {
        
        List<String> chunks = new ArrayList<String>();
        int i, j, len, lenJ, currentLen, bufLen;
        String para, current, chunk, buf, s;
        String[] paragraphs, sentences;

        // 2つの段落単位で、まず分割.
        paragraphs = text.split("\\n\\n+");

        // 段落分割したものを chunkSize 単位で整理する.
        current = "";
        len = paragraphs.length;
        for(i = 0; i < len; i ++) {
            para = paragraphs[i];
            paragraphs[i] = null;
            currentLen = current.length();
            // chunkSize を超える current文字列の場合.
            if(currentLen + para.length() > chunkSize && currentLen > 0) {
                chunks.add(current.trim());
                // [次のcurrent]オーバーラップ: 末尾の一部を次のチャンクに引き継ぐ
                current = new StringBuilder(current.substring(currentLen - overlapSize))
                    .append("\n\n").append(para).toString();
            } else if(current.length() > 0) {
                // 連結元のcurrentが存在する場合.
                current = new StringBuilder(current).append("\n\n").append(para).toString();
            } else {
                // 連結元のcurrentが存在しない場合.
                current = para;
            }
        }
        // currentの余りがある場合.
        current = current.trim();
        if (current.length() > 0) {
            chunks.add(current);
        }

        // チャンクがまだ大きい場合、文単位で再分割
        List<String> result = new ArrayList<String>();
        len = chunks.size();
        for(i = 0; i < len; i ++) {
            chunk = chunks.get(i);
            if (chunk.length() <= chunkSize) {
                result.add(chunk);
            } else {
                // １つの文書の終端を示す区切りで分断する.
                sentences = chunk.split("(?<=[。．.!?！？\\n])");
                buf = "";
                lenJ = sentences.length;
                for(j = 0; j < lenJ; j ++) {
                    s = sentences[j];
                    bufLen = buf.length();
                    if(bufLen + s.length() > chunkSize && bufLen > 0) {
                        result.add(buf.trim());
                        // オーバーラップ: 末尾の一部を次のチャンクに引き継ぐ
                        buf = new StringBuilder(buf.substring(bufLen - overlapSize))
                            .append(s).toString();
                    } else {
                        buf += s;
                    }
                }
                // bufの余りがある場合.
                buf = buf.trim();
                if (buf.length() > 0) {
                    result.add(buf);
                }
            }
        }
        return result;
    }

    // 対象ファイルのファイルタイムを取得.
    public static final long getFileTime(String name) {
        try {
            Path p = Paths.get(name);
            return Files.getLastModifiedTime(p).to(TimeUnit.MILLISECONDS);
        } catch(Exception e) {
            throw new MRagException(e);
        }
    }

    // グループ名から、VectorGroupファイル名を作成する.
    // xxxxx.vgs xxxxxがグループ名の場合のVectorGroupファイル名.
    // group: 対象のグループ名を設定します.
    // 戻り値: グループファイル名が返却されます.
    public static final String getVectorGroupFileName(String group) {
        if(group == null || (group = group.trim()).length() == 0) {
            throw new MRagException("The group name is not set.");
        }
        return group + VECTOR_GROUP_FILE_EXTENSION;
    }

    // VectorGroupファイル名からグループ名を取得.
    // xxxxx.vgs xxxxxがグループ名の場合のVectorGroupファイル名.
    // ここから .vgsのファイル名を削除する. 
    // name: VectorGroupファイル名を設定します.
    // 戻り値: グループ名が返却されます.
    public static final String getVectorGroupFileToGroupName(String name) {
        if(name == null || (name = name.trim()).length() == 0) {
            throw new MRagException("The file name is not set.");
        }
        if(name.endsWith(VECTOR_GROUP_FILE_EXTENSION)) {
            return name.substring(
                0, name.length() - VECTOR_GROUP_FILE_EXTENSION.length())
                .trim();
        }
        throw new MRagException("Not a VectorGroup file name: " + name);
    }

    // 対象パス以下のファイル群を取得.
    // path: 対象のファイルパスを設定します.
    // 戻り値: 対象パス以下のファイル名群を取得します.
    public static final List<String> getPathToFiles(String path) {
        try {
            File f;
            File[] files = new File(path).listFiles();
            int len = files.length;
            List<String> ret = new ArrayList<String>();
            for(int i = 0; i < len; i ++) {
                f = files[i];
                if(f.isFile()) {
                    ret.add(f.getName());
                }
            }
            return ret;
        } catch(Exception e) {
            throw new MRagException(e);
        }
    }

    // VectorGroupファイル情報.
    public static final class VGFileInfo {
        public final String groupName;
        public final String filePath;
        public final String fileName;
        public final long fileTime;
        public VGFileInfo(String group, String path, String name, long time) {
            groupName = group;
            filePath = path;
            fileName = name;
            fileTime = time;
        }
    }

    // 指定パス以下のVectorGroupファイル情報群を取得.
    // path: 対象ディレクトリパスを設定します.
    // 戻り値: VGFileInfoオブジェクトを管理するListオブジェクトが返却されます.
    public static final List<VGFileInfo> getVectorGroupFileNames(String path) {
        int len;
        long time;
        String group, name;
        List<VGFileInfo> ret = new ArrayList<VGFileInfo>();
        // パスの最後に / がある場合は除外.
        if(path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        // 指定パス以下のファイル一覧を取得.
        List<String> files = getPathToFiles(path);
        len = files.size();
        for(int i = 0; i < len; i ++) {
            name = files.get(i);
            // VectorGroupファイルじゃない.
            if(!name.endsWith(VECTOR_GROUP_FILE_EXTENSION)) {
                continue;
            }
            // VectorGroupファイルを生成し、追加する.
            group = getVectorGroupFileToGroupName(name);
            time = getFileTime(path + "/" + name);
            ret.add(new VGFileInfo(group, path, name, time));
        }
        return ret;
    }

    // 対象パスとファイル名を指定して１つのVectorGroupオブジェクトを作成.
    // path: 対象ディレクトリパスを設定します.
    // name: VectorGroupファイル名を設定します.
    // cman: VectorChunkのキャッシュオブジェクトを管理するオブジェクトを設定します.
    // 戻り値: VectorGroupオブジェクトが返却されます.
    public static final VectorGroup loadVectorGroup(
        String path, String name, Queue<VectorChunk> cman) {
        // パスの最後に / がある場合は除外.
        if(path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        if(!name.endsWith(VECTOR_GROUP_FILE_EXTENSION)) {
            // 拡張子が存在しない場合は拡張子をセット.
            name += VECTOR_GROUP_FILE_EXTENSION;
        }
        // ファイル名からグループ名を取得する.
        String group = getVectorGroupFileToGroupName(name);
        // ファイルタイムを取得.
        long time = getFileTime(path + "/" + name);
        // ファイルのロード.
        VectorChunk[] chunks = load(path + "/" + name);
        // vectorGroupを返却.
        return new VectorGroup(group, path, name, time, chunks, cman);
    }

    // 指定パスのファイル名のVectorGroupに対して、ファイルテキストを追加.
    // path: 対象ディレクトリパスを設定します.
    // fileName: VectorGroupファイル名を設定します.
    // textFileName: 新たにVectorGroupに追加するファイル名を設定します.
    // text: 新たにVectorGroupに追加するテキストを設定します.
    // chunkSize: チャンク単位の文字列長を設定します.
    // overlap: 次のチャンクに設定する文字列長を設定します.
    // baseUrl: getEmbedding 対象の http://domain:port までのURLを設定します.
    public static final void addTextFileToVectorGroup(
        String path, String fileName, String textFileName, String text,
        int chunkSize, int overlap, String baseUrl) {
        int i, len, listLen;
        double[] emb;
        String chkTxt;
        VectorChunk[] docs;
        List<VectorChunk> list;
        // パスの最後に / がある場合は除外.
        if(path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        if(!fileName.endsWith(VECTOR_GROUP_FILE_EXTENSION)) {
            // 拡張子が存在しない場合は拡張子をセット.
            fileName += VECTOR_GROUP_FILE_EXTENSION;
        }
        // 対象ファイルが存在するか確認.
        if(new File(path + "/" + fileName).isFile()) {
            // ファイルが存在する場合.
            // 対象条件のVectorGroupを作成.
            VectorGroup vg = loadVectorGroup(path, fileName, null);
            // Vector塊一覧を取得.
            docs = vg.getDocuments();
            vg = null;
            len = docs.length;
            // Vector塊をリスト化.
            list = new ArrayList<VectorChunk>(len);
            for(i = 0; i < len; i ++) {
                // 今回追加対象のtextFileNameが存在する場合は
                // 追加しない.
                if(docs[i].fileName.equals(textFileName)) {
                    continue;
                }
                list.add(docs[i]);
            }
            docs = null;
        } else {
            // ファイルが存在しない場合.
            // 空のlistを作成.
            list = new ArrayList<VectorChunk>();
        }
        // テキストを塊単位で分割する.
        List<String> chunkTextList = stringToChunks(
            text, chunkSize, overlap);
        len = chunkTextList.size();

        // 分割されたテキスト塊の内容をベクトル化.
        // 作成された内容をVectorChunkのリストに追加する.
        List<double[]> vectorList = new ArrayList(len);
        for(i = 0; i < len; i ++) {
            chkTxt = chunkTextList.get(i);
            // 1つのテキストの塊をベクトル座標変換.
            emb = LlamaCpp.getEmbedding(baseUrl, chkTxt);
            // 新しいVectorChunkを追加.
            list.add(
                new VectorChunk(
                    chkTxt, i, len, textFileName, emb)
            );
        }
        // 追加されたlistをVectorChunk配列に変換.
        len = list.size();
        docs = new VectorChunk[len];
        for(i = 0; i < len; i ++) {
            docs[i] = list.get(i);
        }
        // 更新されたdocsを保存する.
        save(path + "/" + fileName, docs);
    }

    // 指定パスのファイル名のVectorGroupに対して、ファイルテキストを削除.
    // path: 対象ディレクトリパスを設定します.
    // fileName: VectorGroupファイル名を設定します.
    // textFileName: 新たにVectorGroupに追加するファイル名を設定します.
    // 戻り値: false の場合削除対象が存在しないことを示します.
    public static final boolean removeTextFileToVectorGroup(
        String path, String fileName, String textFileName) {
        double[] emb;
        int i, len;
        VectorChunk[] docs;
        List<VectorChunk> list;
        // パスの最後に / がある場合は除外.
        if(path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        if(!fileName.endsWith(VECTOR_GROUP_FILE_EXTENSION)) {
            // 拡張子が存在しない場合は拡張子をセット.
            fileName += VECTOR_GROUP_FILE_EXTENSION;
        }
        // 対象ファイルが存在しない場合.
        if(!new File(path + "/" + fileName).isFile()) {
            return false;
        }
        // 対象条件のVectorGroupを作成.
        VectorGroup vg = loadVectorGroup(path, fileName, null);
        // Vector塊一覧を取得.
        docs = vg.getDocuments();
        vg = null;
        len = docs.length;
        // Vector塊をリスト化.
        list = new ArrayList<VectorChunk>(len);
        boolean removeFlag = false;
        for(i = 0; i < len; i ++) {
            // 削除対象の条件の場合.
            if(docs[i].fileName.equals(textFileName)) {
                removeFlag = true;
                continue;
            }
            list.add(docs[i]);
        }
        // 保存対象の条件が存在しない場合.
        if(list.size() == 0) {
            // ファイルの削除.
            try {
                Path p = Paths.get(path + "/" + fileName);
                Files.delete(p);
            } catch(Exception e) {
                // [Linux専用]システムコマンドで強制的に削除.
                try {
                    Runtime.getRuntime().exec(
                        new String[]{ "rm",  "-Rf", 
                            path + "/" + fileName });
                } catch(Exception ee) {
                    // 例外が発生した場合.
                    throw new MRagException(e);
                }
            }
            return true;
        }
        docs = null;
        // 削除条件が存在しない場合.
        if(!removeFlag) {
            return false;
        }
        // 削除済みの条件を元にVectorGroupファイルを保存.
        len = list.size();
        docs = new VectorChunk[len];
        for(i = 0; i < len; i ++) {
            docs[i] = list.get(i);
        }
        // 更新されたdocsを保存する.
        save(path + "/" + fileName, docs);
        return true;
    }
}
