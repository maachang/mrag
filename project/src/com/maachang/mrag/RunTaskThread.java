package com.maachang.mrag;

// タスク実行用ループスレッド用 Runnable.
public abstract class RunTaskThread implements RunTask, Runnable {
	// 停止スレッドフラグ.
	protected volatile boolean stopFlag = false;
	// 終了スレッドフラグ.
	protected volatile boolean exitFlag = false;
	// 実行バーチャルスレッドモード.
	protected boolean vThreadMode = false;

	// 実行スレッドBuilder.
	protected Thread.Builder threadBuilder = null;

	// バーチャルスレッド実施かチェック.
	// 戻り値: trueの場合、バーチャルスレッドで実行されている.
	public boolean isVThread() {
		return vThreadMode;
	}

	// プラットフォームスレッド開始.
	public void startThread() {
		if(threadBuilder != null) {
			throw new MRagException(
				"The thread has already been created and is running.");
		}
		// プラットフォームスレッドで実施.
		vThreadMode = false;
		// プラットフォームスレッドを生成して実行.
		threadBuilder = Thread.ofPlatform();
		// スレッド開始.
		_startThread();
	}

	// バーチャルフォームスレッド開始.
	public void startVThread() {
		if(threadBuilder != null) {
			throw new MRagException(
				"The thread has already been created and is running.");
		}
		// バーチャルフォームスレッドで実施.
		vThreadMode = true;
		// バーチャルフォームスレッドを生成して実行.
		threadBuilder = Thread.ofVirtual();
		// スレッド開始.
		_startThread();
	}

	// スレッド開始共通.
	private void _startThread() {
		// プラットフォームスレッドの場合.
		if(threadBuilder instanceof Thread.Builder.OfPlatform) {
			// daemonをtrue.
			((Thread.Builder.OfPlatform)threadBuilder).daemon(true);
		}
		threadBuilder.start(this);
	}

	// スレッド終了.
	public void stopThread() {
		stopFlag = true;
	}

	// スレッド停止が呼び出されているかチェック.
	public boolean isStopThread() {
		return stopFlag;
	}

	// スレッドが終了しているか.
	public boolean isExitThread() {
		return exitFlag;
	}

	// スレッド開始時の実行処理.
	public void initThread() {
		// 何もしない.
	}

	// スレッドエラー時の実行処理.
	public void errorThread(Throwable e) {
		// 何もしない.
	}

	// スレッド終了時の実行処理.
	public void exitThread() {
		// 何もしない.
	}

	// １度のタスク実行.
	public abstract void executeTask();

	// タスクエラー実行.
	public void errorTask(Throwable e) {
		// 何もしない.
	}

	// タスク終了実行.
	public void exitTask() {
		// 何もしない.
	}

	// 現在の未処理のタスク数を取得.
	// 戻り値: 現在の未処理のタスク数を取得が返却されます.
	//         -1が返却された場合、未対応です.
	public int size() {
		return -1;
	}

	// スリープ実行.
	public void sleepThread(long time) {
		try {
			Thread.sleep(time);
		} catch(Exception e) {}
	}

	// スレッド開始.
	public void run() {
		try {
			// スレッド開始時に実行.
			initThread();
			// 停止フラグが false の間は実行しつづける.
			while (!stopFlag) {
				try {
					// タスクを実行.
					executeTask();
				} catch(Throwable e) {
					try {
						// タスクでエラー発生の場合.
						errorTask(e);
					} catch(Exception ee) {}
				} finally {
					try {
						// タスク終了実行.
						exitTask();
					} catch(Exception ee) {}
				}
			}
		} catch(Throwable e) {
			try {
				// スレッドがエラーになった場合の呼び出し処理.
				errorThread(e);
			} catch(Exception ee) {}
		} finally {
			try {
				// スレッド終了時の処理.
				exitThread();
			} catch(Exception ee) {}
			threadBuilder = null;
			exitFlag = true;
		}
    }
}