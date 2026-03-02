package com.maachang.mrag.vector.task;

import java.util.*;

import com.maachang.mrag.vector.*;
import com.maachang.mrag.*;
import com.maachang.mrag.Util.SortKeyValue;

// VectorStoreでの推論問い合わせに対するタスク.
public class VectorChatTask implements VectorGroupTask {
    // 削除Vectorグループ名.
    private VectorGroup vectorGroup;
    // 推論問い合わせメッセージ.
    private String message;
    // タスク実行結果をコールバックするオブジェクト.
    private TaskCallback taskCall;

    // コンストラクタ.
    // vectorGroup: 実行VectorGroupオブジェクトを設定します.
    // message: 推論対象のメッセージを設定します.
    // taskCall: 推論結果を返却実行対象のコールバックオブジェクトを設定します.
    public VectorChatTask(
        VectorGroup vectorGroup, String message, TaskCallback taskCall) {
        this.vectorGroup = vectorGroup;
        this.message = message;
        this.taskCall = taskCall;
    }

    // グループ名を取得.
    // 戻り値: グループ名が返却されます.
    public String getGroupName() {
        return vectorGroup.getGroup();
    }

	// タスク実行.
	public void executeTask() {
        Config config = Config.getInstance();

        // 質問内容を順位としてのVectorChunk群に変換.
        VectorChunk[] searchResult = VectorFile.searchEmbedding(
            vectorGroup, config.chunkSize, config.overlapSize,
            config.vectorSearchLength, message);
        
        // 検索条件を定めた文書名群を取得.
        SortKeyValue[] docList = sortToScore(
            searchResult, config.vectorSearchLength);

        // 検索文書名を検索.
        int maxLen = docList.length >= config.ragRequestChunkLength ?
            config.ragRequestChunkLength : docList.length;

        // 組み込み結果の検索文書候補文字列を作成.
        String docName;
        String docUrl;
        String summaryTxt;
        SortKeyValue n;
        VectorSummary vs;
        StringBuilder embMsg = new StringBuilder();
        for(int i = 0; i < maxLen; i ++) {
            n = docList[i];
            docName = (String)n.value;
            vs = vectorGroup.getSummary();
            summaryTxt = vs.getText(docName);
            docUrl = vs.getUrl(docName);
            embMsg.append(config.getRagRequestChunk(
                (i + 1), docName, docUrl, (Float)n.key, summaryTxt));
        }
        n = null; docName = null; vs = null; summaryTxt = null; docUrl = null;

        // 推論用のプロンプトを作成.
        String prompt = config.getRagRequest(embMsg.toString(), message);
        embMsg = null;

        // 推論実行.
        Object resChatJson = LlamaCpp.getChatCompletions(
            config.getChatURL(),
            prompt, config.ragTemperature, -1);
        
        // 推論結果を取得.
        taskCall.call(
            message // 質問をセット.
            ,LlamaCpp.getResultChatCompletionsToText(
                resChatJson) // 推論結果のメッセージ.
            ,resChatJson // 推論結果のjson.
        );
    }

    // VectorGroup.serach結果を得点集計して返却.
    private static final SortKeyValue[] sortToScore(VectorChunk[] result, int resultLen) {
        // ベクトル検索結果の順位のための計算処理を実施.
        SortKeyValue n;
        Map<String, SortKeyValue> ranking = new HashMap<String, SortKeyValue>();
        List<SortKeyValue> sortList = Util.createSortKeyValueList();
        for(int i = 0; i < resultLen; i++) {
            VectorChunk v = result[i];
            n = ranking.get(v.docName);
            if(n == null) {
                // 一番高い得点の数字の対象抽出.
                n = new SortKeyValue(v.score, v.docName);
                ranking.put(v.docName, n);
                sortList.add(n);
            }
        }
        // 降順でソート処理.
        return Util.sortKeyValues(sortList, false);
    }
}