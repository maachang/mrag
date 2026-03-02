package com.maachang.mrag.vector.task;

import java.util.*;
import java.util.concurrent.*;

import com.maachang.mrag.vector.*;
import com.maachang.mrag.*;

// Vectorグループの更新を行う.
public class UpdateVectorGroupTask implements VectorGroupTask {
	// VectorStoreGroupリスト管理.
	private Map<String, VectorGroup> vectorGroupList;

    // VectorChunkキャッシュオブジェクト.
    private Queue<VectorChunk> cacheMan;

    // 更新Vectorグループ名.
    private String groupName;

    // コンストラクタ.
    // vgList: VectorStoreGroupリスト管理を設定します.
    // cman: VectorChunkキャッシュオブジェクトを設定します.
    // name: 更新するグループ名を設定します.
    public UpdateVectorGroupTask(
        Map<String, VectorGroup> vgList, Queue<VectorChunk> cman, String name) {
        vectorGroupList = vgList;
        cacheMan = cman;
        groupName = name;
    }

	// タスク実行.
	public void executeTask() {
        Config cf = Config.getInstance();
        // VectorGroupを取得.
        VectorGroup newvVg = VectorFile.loadVectorGroup(
            cf.vectorStorePath, groupName, cacheMan);
        // VectorStoreGroupListに上書き.
        vectorGroupList.put(groupName, newvVg);
    }

    // グループ名を取得.
    // 戻り値: グループ名が返却されます.
    public String getGroupName() {
        return groupName;
    }
}