package com.maachang.mrag;

// タスクコールバック実行.
public interface TaskCallback {
    
    // タスク結果のコールバック実行.
    // args: コールバックに渡されるパラメータを設定します.
    public void call(Object... args);
}