package com.maachang.mrag.vector;

import java.util.*;
import java.util.concurrent.*;

import com.maachang.mrag.*;
import com.maachang.mrag.vector.task.*;
import com.maachang.mrag.vector.VectorFile.VGFileInfo;

// VectorStoreに対するタスク実行. 
// こちらは startThreadで動かす.
public class VectorTaskThread extends RunTaskThread {
	// 非タスク時の待機時間.
	private static final long WAIT_TIME = 1000L;

    // タスクQueue.
    private Queue<RunTask> queue =
		new ConcurrentLinkedQueue<RunTask>();
	// Weitオブジェクト.
	private Wait wait = new Wait();

	// 実行中のタスク数.
	private final AInt runTaskCount = new AInt();

	// group単位のvertualThread管理.
	private Map<String, VectorGroupTaskThread> groupThreadList =
		new HashMap<String, VectorGroupTaskThread>(); 

	// VectorStoreGroupリスト管理.
	private Map<String, VectorGroup> vectorGroupList;

	// VectorChunkキャッシュ管理.
	private Queue<VectorChunk> cacheMan;

	// グループファイル管理リスト.
	private Map<String, VGFileInfo> groupFileList =
		new ConcurrentHashMap<String, VGFileInfo>();

	// グループファイルリスト確認タイミング.
	// 5秒に１度確認.
	private static final long CHECK_GROUP_FILE_LIST = 5000L;

	// 前回グループファイル管理リストの実行時間.
	private long lastGroupFileListTime = -1L;

	// コンストラクタ.
	// vsList: VectorStoreオブジェクトで管理している
	//         VectorStoreGroupリストを設定します.
	// cman: VectorChunkキャッシュ管理オブジェクトを設定します.
	public VectorTaskThread(
		Map<String, VectorGroup> vsList, Queue<VectorChunk> cman) {
		vectorGroupList = vsList;
		cacheMan = cman;
	}

	// 外部処理からのタスク追加.
	// rt: 追加対象のタスクを設定します.
	protected void addTask(RunTask rt) {
		// タスクをqueueに追加.
		queue.offer(rt);
		// waitの解除.
		wait.signal();
	}

	// グループファイルリストを取得します.
	// 戻り値: グループファイルリストが返却されます.
	public Map<String, VGFileInfo> getGroupFileList() {
		return groupFileList;
	}

	// [スレッド実行]タスク実行.
	public void executeTask() {
		// 更新タスク確認.
		updateGroupFileListTask();

		// 先頭のタスクを取得.
		RunTask task = queue.poll();
		// タスクが存在しない場合.
		if(task == null) {
			// wait処理.
			wait.await(WAIT_TIME);
			return;
		}

		try {
			// 実行中タスクカウント:1 ++
			runTaskCount.inc();

			// 対象タスクがVectorGroupTaskの場合
			// 専用のグループタスク処理で行うように割り振る.
			if(task instanceof VectorGroupTask) {
				setVectorGroupTask((VectorGroupTask)task);
				return;
			}

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

	// [スレッド実行]グループファイル管理リストを更新.
	// 更新後必要な処理のタスクを作成.
	// この処理は CHECK_GROUP_FILE_LIST ミリ秒(5秒)に１回実行される.
	private void updateGroupFileListTask() {
		// グループファイル管理リストの確認が不要な場合.
		// 前回実行から CHECK_GROUP_FILE_LIST ミリ秒(5秒) 経過していない.
		if(lastGroupFileListTime != -1L &&
			lastGroupFileListTime + CHECK_GROUP_FILE_LIST > System.currentTimeMillis()) {
			return;
		}
		// 更新確認.
		List<String> updateGroups = VectorFile.updateVectorGroupFileNames(
			groupFileList, Config.getInstance().vectorStorePath);
		
		// 更新時間を更新.
		lastGroupFileListTime = System.currentTimeMillis();

		// 更新条件が存在しない場合.
		if(updateGroups.size() <= 0) {
			return;
		}

		// 更新処理を実施するタスクを生成.
		String group;
		int len = updateGroups.size();
		for(int i = 0; i < len; i ++) {
			group = updateGroups.get(i);
			// グループ名が走査したグループファイルリストに存在する場合.
			if(groupFileList.containsKey(group)) {
				// 存在する場合は更新処理.
				queue.offer(
					new UpdateVectorGroupTask(
						vectorGroupList, cacheMan, group));
			} else {
				// 存在しない場合は削除処理.
				queue.offer(
					new DeleteVectorGroupTask(
						vectorGroupList, group));
			}
		}
	}

	// vectorGroupTaskの場合、専用のタスク実行に振り分ける.
	// task 追加対象のVectorGroupTaskを設定します.
	private void setVectorGroupTask(VectorGroupTask task) {
		// グループ名を取得.
		String groupName = task.getGroupName();
		// グループタスクを取得.
		VectorGroupTaskThread tt = groupThreadList.get(groupName);

		// グループタスクスレッドが存在しない場合.
		if(tt == null) {
			// グループタスクスレッドを作成して保存する.
			tt = new VectorGroupTaskThread();
			tt.startVThread();
			groupThreadList.put(groupName, tt);
		}

		// グループタスクスレッドに対象タスクをセットする.
		tt.addTask(task);
	}
}