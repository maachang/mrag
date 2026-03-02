package com.maachang.mrag;

import java.io.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.*;

// waitオブジェクト.
public class Wait {

	// ロックオブジェクト.
	private final Lock sync = new ReentrantLock();
	private final Condition con = sync.newCondition();
	private final AInt awaitFlag = new AInt(0);

	//コンストラクタ.
	public Wait() {
	}

	// 指定時間待機.
	public final void await() {
		sync.lock();
		try {
			awaitFlag.inc(); // セット.
			con.await();
        } catch(Exception e) {
            throw new MRagException(e);
		} finally {
			sync.unlock();
			awaitFlag.dec(); // 解除.
		}
	}

	// 指定時間待機.
	// timeout ミリ秒での待機時間を設定します.
    //         [0]を設定した場合、無限待機となります.
	// 戻り値: [true]が返された場合、復帰条件が設定されました.
	public final boolean await(long time) {
        try {
            if (time <= 0L) {
                await();
                return true;
            } else {
                sync.lock();
                try {
                    awaitFlag.inc(); // セット.
                    return con.await(time, TimeUnit.MILLISECONDS);
                } finally {
                    sync.unlock();
                    awaitFlag.dec(); // 解除.
                }
            }
        } catch(Exception e) {
            throw new MRagException(e);
        }
	}

	// 待機中のスレッドを１つ起動.
	public final void signal() {
		if (awaitFlag.get() > 0) {
			sync.lock();
			try {
				con.signal();
            } catch(Exception e) {
                throw new MRagException(e);
			} finally {
				sync.unlock();
			}
		}
	}

	// 待機中のスレッドを全て起動.
	public final void signalAll() {
		if (awaitFlag.get() > 0) {
			sync.lock();
			try {
				con.signalAll();
            } catch(Exception e) {
                throw new MRagException(e);
			} finally {
				sync.unlock();
			}
		}
	}

	// 現在待機中かチェック.
	// 戻り値: [true]の場合、待機中です.
	public final boolean isWait() {
		return awaitFlag.get() > 0;
	}

	// ロックオブジェクトの取得.
	// 戻り値: ロックオブジェクトが返却されます.
	public final Lock getLock() {
		return sync;
	}
}