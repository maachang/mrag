package com.maachang.mrag.vector;

import java.io.*;
import java.util.*;
import java.nio.file.*;
import java.util.stream.*;
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

    // VectorSummaryファイルシンボル.
    private static final String VECTOR_SUMMARY_FILE_SIMBOL = "@vss";

    // VectorSummaryファイル拡張子.
    public static final String VECTOR_SUMMARY_FILE_EXTENSION = ".vss";

    // シンボルの文字数.
    private static final int SIMBOLE_SIZE = 4;

    // 拡張子の文字数.
    private static final int FILE_EXTENSION_SIZE = 4;

    // llama.cppの推論でファイル内容をサマリー化文言.
    private static final String SUMMARY_LLM_HEAD_MSG = "以下の内容のサマリーをまとめてほしい。\n\n ---\n ";

    // path と groupNameを整頓.
    private static final String[] trimPathGroupToFilePath(
        String path, String groupName) {
        return (String[])_getPathGroupToFilePath(path, groupName, null);
    }

    // ファイルパスを取得.
    private static final String getPathGroupToFilePath(
        String path, String groupName, String extension) {
        return (String)_getPathGroupToFilePath(path, groupName, extension);
    }

    // ファイルパス or path と groupNameを整頓.
    private static final Object _getPathGroupToFilePath(
        String path, String groupName, String extension) {
        // パスの最後に / がある場合は除外.
        if(path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        // 拡張子が指定されている場合.
        if(groupName.endsWith(VECTOR_GROUP_FILE_EXTENSION) ||
            groupName.endsWith(VECTOR_SUMMARY_FILE_EXTENSION)) {
            // 拡張子名を除外する.
            groupName = groupName.substring(
                0, groupName.length() - FILE_EXTENSION_SIZE);
        }
        // 拡張子が指定されていない場合.
        if(extension == null) {
            return new String[] {path, groupName};
        }
        return path + "/" + groupName + extension;
    }

    // VectorChunk群をファイルロード処理.
    // path: 対象ディレクトリパスを設定します.
    // groupName: グループ名を設定します.
    // 戻り値: VectorChunk[] が返却されます.
    public static final VectorChunk[] loadGroup(String path, String groupName) {
        // ファイルパスを取得.
        String fileName = getPathGroupToFilePath(
            path, groupName, VECTOR_GROUP_FILE_EXTENSION);
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
        return loadGroup(b);
    }

    // VectorChunk群をバイナリロード処理.
    // binary: バイナリを指定してロード処理を行います.
    // 戻り値: VectorChunk[] が返却されます.
    public static final VectorChunk[] loadGroup(byte[] binary) {
        DecodeBinary bd = new DecodeBinary(binary);
        // ファイルシンボルの確認.
        String simbol = bd.getString(SIMBOLE_SIZE);
        if(!VECTOR_GROUP_FILE_SIMBOL.equals(simbol)) {
            throw new MRagException("Not a VectorGroup file symbol");
        }
        // 最初にVectorChunk数を取得.
        int allLen = bd.getUInt3();
        VectorChunk[] ret = new VectorChunk[allLen];
        // binary化されてるVectorChunk群をdeSerialize.
        int i, j, indexNo, len;
        String fileName, text;
        float[] embList;
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
            embList = new float[len];
            for(j = 0; j < len; j ++) {
                // 1つのembeddingを取得.
                embList[j] = bd.getFloat();
            }
            ret[i] = new VectorChunk(
                text, indexNo, allLen, fileName, embList);
        }
        return ret;
    }

    // VectorChunk群をファイルに保存(serialize)
    // path: 対象ディレクトリパスを設定します.
    // groupName: グループ名を設定します.
    // chunks: 保存対象の VectorChunk 群を設定します.
    public static final void saveGroup(
        String path, String groupName, VectorChunk[] chunks) {
        // ファイルパスを取得.
        String fileName = getPathGroupToFilePath(
            path, groupName, VECTOR_GROUP_FILE_EXTENSION);
        FileOutputStream fo = null;
        BufferedOutputStream bo = null;
        try {
            fo = new FileOutputStream(fileName);
            bo = new BufferedOutputStream(fo);
            saveGroup(bo, chunks);
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
    public static final void saveGroup(OutputStream out, VectorChunk[] chunks) {
        int i, j, len;
        byte[] bin;
        float[] embList;
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
                    out.write(EncodeBinary.getFloat(embList[i]));
                }
            }
            out.flush();
        } catch(Exception e) {
            throw new MRagException(e);
        }
    }

    // VectorSummaryをファイルロード処理.
    // path: 対象ディレクトリパスを設定します.
    // groupName: グループ名を設定します.
    // 戻り値: VectorSummary が返却されます.
    public static final VectorSummary loadSummary(
        String path, String groupName) {
        // ファイルパスを取得.
        String fileName = getPathGroupToFilePath(
            path, groupName, VECTOR_SUMMARY_FILE_EXTENSION);
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
        return loadSummary(b);
    }

    // VectorSummaryをバイナリロード処理.
    // binary: バイナリを指定してロード処理を行います.
    // 戻り値: VectorSummary が返却されます.
    public static final VectorSummary loadSummary(byte[] binary) {
        int i, len;
        String fileName, text;
        DecodeBinary bd = new DecodeBinary(binary);
        // ファイルシンボルの確認.
        String simbol = bd.getString(SIMBOLE_SIZE);
        if(!VECTOR_SUMMARY_FILE_SIMBOL.equals(simbol)) {
            throw new MRagException("Not a VectorSummary file symbol");
        }
        // VectorSummaryを生成.
        VectorSummary ret = new VectorSummary();
        // 最初にVectorSummary数を取得.
        int allLen = bd.getUInt3();
        for(i = 0; i < allLen; i ++) {
            // ファイル名を取得.
            len = bd.getUInt2();
            fileName = bd.getString(len);
            // テキストを取得.
            len = bd.getUInt3();
            text = bd.getString(len);
            ret.put(fileName, text);
        }
        return ret;
    }

    // VectorSummaryをファイルに保存(serialize)
    // path: 対象ディレクトリパスを設定します.
    // groupName: グループ名を設定します.
    // chunks: 保存対象の VectorSummaryを設定します.
    public static final void saveSummary(
        String path, String groupName, VectorSummary summary) {
        // ファイルパスを取得.
        String fileName = getPathGroupToFilePath(
            path, groupName, VECTOR_SUMMARY_FILE_EXTENSION);
        FileOutputStream fo = null;
        BufferedOutputStream bo = null;
        try {
            fo = new FileOutputStream(fileName);
            bo = new BufferedOutputStream(fo);
            saveSummary(bo, summary);
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

    // VectorSummaryを保存(serialize)
    // 保存先のOutputStreamを設定します.
    // summary: 保存対象の VectorSummaryを設定します.
    public static final void saveSummary(OutputStream out, VectorSummary summary) {
        int i, len;
        byte[] bin;
        String fileName, text;
        String[] names = summary.getNames();
        int allLen = names.length;
        try {
            // ファイルシンボルを出力.
            out.write(EncodeBinary.getString(VECTOR_SUMMARY_FILE_SIMBOL));
            // 最初にVectorChunk数を保存.
            out.write(EncodeBinary.getInt3(allLen));
            // 保存対象のVectorChunk群をループ実行.
            for(i = 0; i < allLen; i ++) {
                fileName = names[i];
                text = summary.get(fileName);
                // ファイル名を保存.
                bin = EncodeBinary.getString(fileName);
                out.write(EncodeBinary.getInt2(bin.length));
                out.write(bin);
                // テキストを保存.
                bin = EncodeBinary.getString(text);
                out.write(EncodeBinary.getInt3(bin.length));
                out.write(bin);
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
        int i, len, bufLen;
        String buf, s;
        String[] sentences;

        // チャンクがまだ大きい場合、文単位で再分割
        List<String> result = new ArrayList<String>();
        // １つの文書の終端を示す区切りで分断する.
        sentences = text.split("(?<=[。!?！？\\n])");
        buf = "";
        len = sentences.length;
        for(i = 0; i < len; i ++) {
            s = sentences[i];
            bufLen = buf.length();
            // バッファサイズを超える場合.
            if(bufLen > chunkSize) {
                // chunkSize 分を追加.
                result.add(buf.substring(0, chunkSize).trim());
                // オーバーラップ: 末尾の一部を次のチャンクに引き継ぐ
                buf = buf.substring(chunkSize - overlapSize);
            }
            // 今回追加する内容でバッファサイズを超える場合.
            else if(bufLen + s.length() > chunkSize && bufLen > 0) {
                result.add(buf.trim());
                // オーバーラップ: 末尾の一部を次のチャンクに引き継ぐ
                buf = new StringBuilder(buf.substring(bufLen - overlapSize))
                    .append(s).toString();
            }
            // バッファを超えない場合.
            else {
                buf += s;
            }
        }
        // bufの余りがある場合.
        buf = buf.trim();
        if (buf.length() > 0) {
            while(true) {
                bufLen = buf.length();
                // 残りバッファが chunkSize以内の場合.
                if(bufLen <= chunkSize) {
                    result.add(buf.trim());
                    break;
                }
                // chunkSize 分を追加.
                result.add(buf.substring(0, chunkSize).trim());
                // オーバーラップ: 末尾の一部を次のチャンクに引き継ぐ
                buf = buf.substring(chunkSize - overlapSize);
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

    // 対象パスとファイル名を指定して１つのVectorSummaryオブジェクトを作成.
    // path: 対象ディレクトリパスを設定します.
    // groupName: グループ名を設定します.
    // 戻り値: VectorSummaryオブジェクトが返却されます.
    public static final VectorSummary loadVectorSummary(
        String path, String groupName) {
        //パスとグループを整頓.
        String[] pg = (String[])trimPathGroupToFilePath(path, groupName);
        path = pg[0]; groupName = pg[1]; pg = null;
        // VectorSummaryを返却.
        return loadSummary(path, groupName);
    }

    // 対象パスとファイル名を指定して１つのVectorGroupオブジェクトを作成.
    // この処理でVectorSummaryもロードされます.
    // path: 対象ディレクトリパスを設定します.
    // groupName: グループ名を設定します.
    // cman: VectorChunkのキャッシュオブジェクトを管理するオブジェクトを設定します.
    // 戻り値: VectorGroupオブジェクトが返却されます.
    public static final VectorGroup loadVectorGroup(
        String path, String groupName, Queue<VectorChunk> cman) {
        //パスとグループを整頓.
        String[] pg = (String[])trimPathGroupToFilePath(path, groupName);
        path = pg[0]; groupName = pg[1]; pg = null;
        // VectorGroupファイル名.
        String vgFileName = groupName + VECTOR_GROUP_FILE_EXTENSION;
        // ファイルタイムを取得.
        long time = getFileTime(path + "/" + vgFileName);
        // ファイルのロード.
        VectorChunk[] chunks = loadGroup(path, groupName);
        // VectorSummaryファイルをロード.
        VectorSummary summary = loadVectorSummary(
            path, groupName);
        // vectorGroupを返却.
        return new VectorGroup(
            groupName, path, vgFileName, time, chunks, summary, cman);
    }

    // ファイルが存在するか確認.
    private static final boolean isFile(String path, String fileName) {
        return new File(path + "/" + fileName).isFile();
    }

    // 指定パスのファイル名のVectorGroupに対して、ファイルテキストを追加.
    // path: 対象ディレクトリパスを設定します.
    // groupName: グループ名を設定します.
    // textFileName: 新たにVectorGroupに追加するファイル名を設定します.
    // text: 新たにVectorGroupに追加するテキストを設定します.
    // chunkSize: チャンク単位の文字列長を設定します.
    // overlap: 次のチャンクに設定する文字列長を設定します.
    // embBaseUrl: getEmbedding 対象の http://domain:port までのURLを設定します.
    // chBaseUrl: getChatCompletions 対象の http://domain:port までのURLを設定します.
    public static final void addTextFileToVectorGroup(
        String path, String groupName, String textFileName, String text,
        int chunkSize, int overlap, String embBaseUrl, String chBaseUrl) {
        int i, len, listLen;
        float[] emb;
        String chkTxt;
        VectorChunk[] docs;
        VectorSummary summary;
        List<VectorChunk> list;

        //パスとグループを整頓.
        String[] pg = (String[])trimPathGroupToFilePath(path, groupName);
        path = pg[0]; groupName = pg[1]; pg = null;
        // vectorGroupファイル名.
        String vgFileName = groupName + VECTOR_SUMMARY_FILE_EXTENSION;
        // vectorSummaryファイル名.
        String vsFileName = groupName + VECTOR_SUMMARY_FILE_EXTENSION;
        // 対象VectorGroupファイルが存在するか確認.
        if(isFile(path, vgFileName)) {
            // 一方でVectorSummaryファイルが存在しない場合はエラー.
            if(!isFile(path, vsFileName)) {
                throw new MRagException(
                    "Target VectorSummary file does not exist: " + vsFileName);
            }
            // ファイルが存在する場合.
            // 対象条件のVectorGroupを作成.
            VectorGroup vg = loadVectorGroup(
                path, groupName, null);
            // Vector塊一覧を取得.
            docs = vg.getDocuments();
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

            // VectorSummaryを取得.
            summary = vg.getSummary();
        } else {
            // ファイルが存在しない場合.
            // 空のlistを作成.
            list = new ArrayList<VectorChunk>();
            // 空のVectorSummaryファイルを生成.
            summary = new VectorSummary();
        }

        // サマリー文書を取得.
        String sumTxt = LlamaCpp.getChatMessage(
            chBaseUrl, SUMMARY_LLM_HEAD_MSG + text);
        
        // サマリー文書を加工.
        sumTxt = Conv.stripMarkdown(sumTxt); // マークダウンを除去.
        sumTxt = Conv.exclusionText(sumTxt); // 不要な文字を除去.
        sumTxt = Conv.trimEnterText(sumTxt); // 不要な改行を除去.

        // 作成したサマリー情報を追加.
        summary.put(textFileName, sumTxt);

        // 分解するテキストもサマリーに変更.
        text = sumTxt;

        // テキストを塊単位で分割する.
        List<String> chunkTextList = stringToChunks(
            text, chunkSize, overlap);
        len = chunkTextList.size();

        // 分割されたテキスト塊の内容をベクトル化.
        // 作成された内容をVectorChunkのリストに追加する.
        List<float[]> vectorList = new ArrayList<float[]>(len);
        for(i = 0; i < len; i ++) {
            chkTxt = chunkTextList.get(i);
            // 1つのテキストの塊をベクトル座標変換.
            emb = LlamaCpp.getEmbedding(embBaseUrl, chkTxt);
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
        saveGroup(path, groupName, docs);
        // 更新されたsummaryを保存する.
        saveSummary(path, groupName, summary);
    }

    // 指定パスのファイル名のVectorGroupに対して、ファイルテキストを削除.
    // path: 対象ディレクトリパスを設定します.
    // groupName: グループ名を設定します.
    // textFileName: 新たにVectorGroupに追加するファイル名を設定します.
    // 戻り値: false の場合削除対象が存在しないことを示します.
    public static final boolean removeTextFileToVectorGroup(
        String path, String groupName, String textFileName) {
        float[] emb;
        int i, len;
        VectorChunk[] docs;
        List<VectorChunk> list;

        //パスとグループを整頓.
        String[] pg = (String[])trimPathGroupToFilePath(path, groupName);
        path = pg[0]; groupName = pg[1]; pg = null;
        // vectorGroupファイル名.
        String vgFileName = groupName + VECTOR_SUMMARY_FILE_EXTENSION;
        // vectorSummaryファイル名.
        String vsFileName = groupName + VECTOR_SUMMARY_FILE_EXTENSION;
        // どちらかの対象ファイルが存在しない場合.
        boolean vgFile = isFile(path, vgFileName);
        boolean vsFile = isFile(path, vsFileName);
        if(!vgFile || !vsFile) {
            // 両方のファイルが存在しない場合は正常扱い.
            if(vgFile && vsFile) {
                return false;
            } else if(!vgFile) {
                // VectorGroupファイルが存在しない.
                throw new MRagException(
                    "VectorGroup file does not exist: " + groupName);
            } else if(!vsFile) {
                // VectorSummaryファイルが存在しない.
                throw new MRagException(
                    "VectorSummary file does not exist: " + groupName);
            }
        }
        // 対象条件のVectorGroupを作成.
        // 仮に生成するだけなのでVectorSummaryは空でよい.
        VectorGroup vg = loadVectorGroup(
            path, vgFileName, null);
        // VectorSummaryを取得.
        VectorSummary vs = vg.getSummary();
        // Vector塊一覧を取得.
        docs = vg.getDocuments();
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
            // VectorGroupファイルの削除.
            MRagException mre1 = _removeFile(path, vgFileName);
            // VectorSummaryファイルの削除.
            MRagException mre2 = _removeFile(path, vsFileName);
            // 削除時にエラーが発生している場合.
            if(mre1 != null) {
                throw mre1;
            } else if(mre2 != null) {
                throw mre2;
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
        saveGroup(path, groupName, docs);
        // VectorSummaryから削除ファイル名を指定して削除.
        vs.getList().remove(textFileName);
        // VectorSummaryを保存.
        saveSummary(path, groupName, vs);
        return true;
    }

    // ファイル削除処理.
    private static final MRagException _removeFile(
        String path, String fileName) {
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
                return new MRagException(ee);
            }
        }
        return null;
    }

    // 対象パス以下のファイル群を取得.
    // path: 対象のファイルパスを設定します.
    // 戻り値: 対象パス以下のファイル名群を取得します.
    private static final List<String> getPathToFiles(String path) {
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
            // グループ名を取得.
            group = name.substring(0, name.length() - FILE_EXTENSION_SIZE);
            // VectorGroupファイルを生成し、追加する.
            time = getFileTime(path + "/" + name);
            ret.add(new VGFileInfo(group, path, name, time));
        }
        return ret;
    }


}
