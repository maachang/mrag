package com.maachang.mrag.vector;

import java.util.*;
import java.util.concurrent.*;

import com.maachang.mrag.*;
import com.maachang.mrag.vector.task.*;

// VectorStoreに対するタスク実行. 
// こちらは startVThreadで動かす.
public class VectorGroupTaskThread extends RunTaskThread {
	// 非タスク時の待機時間.
	private static final long WAIT_TIME = 1000L;

    // VectorGroup専用タスクQueue.
    private final Queue<VectorGroupTask> queue =
		new ConcurrentLinkedQueue<VectorGroupTask>();
	// Weitオブジェクト.
	private final Wait wait = new Wait();

	// 実行中のタスク数.
	private final AInt runTaskCount = new AInt();

	// コンストラクタ.
	public VectorGroupTaskThread() {
	}

	// 外部処理からのタスク追加.
	// rt: 追加対象のタスクを設定します.
	protected void addTask(VectorGroupTask rt) {
		// タスクをqueueに追加.
		queue.offer(rt);
		// waitの解除.
		wait.signal();
	}

	// [スレッド実行]タスク実行.
	public void executeTask() {

		// 先頭のタスクを取得.
		VectorGroupTask task = queue.poll();
		// タスクが存在しない場合.
		if(task == null) {
			// wait処理.
			wait.await(WAIT_TIME);
			return;
		}

		try {
			// 実行中タスクカウント:1 ++
			runTaskCount.inc();
			// タスク実行.
			// 実行タスクでエラーが発生した場合の対応は別途考える: todo.
			task.executeTask();
		} finally {
			// 実行中のタスクカウント:1--
			runTaskCount.dec();
		}
	}

	// 現在のタスク数を取得.
	// 戻り値: 現在のタスク数と実行中の数が合わせて返却されます.
	public int size() {
		return queue.size() + runTaskCount.get();
	}
}