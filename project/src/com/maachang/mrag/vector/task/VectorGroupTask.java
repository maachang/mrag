package com.maachang.mrag.vector.task;

import com.maachang.mrag.*;

// VectorGroup用のタスク.
// これを継承したオブジェクトは
//  - VectorGroupTaskThreadで実行
// されます.
public interface VectorGroupTask extends RunTask {
    // グループ名を取得.
    // 戻り値: グループ名が返却されます.
    public String getGroupName();
}