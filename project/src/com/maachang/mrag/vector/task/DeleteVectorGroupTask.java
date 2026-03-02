package com.maachang.mrag.vector.task;

import java.util.*;

import com.maachang.mrag.vector.*;
import com.maachang.mrag.*;

// Vectorグループの削除を行う.
public class DeleteVectorGroupTask implements VectorGroupTask {
	// VectorStoreGroupリスト管理.
	private Map<String, VectorGroup> vectorGroupList;

    // 削除Vectorグループ名.
    private String groupName;

    // コンストラクタ.
    // vgList: VectorStoreGroupリスト管理を設定します.
    // name: 削除するグループ名を設定します.
    public DeleteVectorGroupTask(
        Map<String, VectorGroup> vgList, String name) {
        vectorGroupList = vgList;
        groupName = name;
    }

	// タスク実行.
	public void executeTask() {
        // VectorStoreGroupListから削除.
        vectorGroupList.remove(groupName);
    }

    // グループ名を取得.
    // 戻り値: グループ名が返却されます.
    public String getGroupName() {
        return groupName;
    }
}