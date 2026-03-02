package com.maachang.mrag;

import java.util.concurrent.atomic.*;

// atomicInt.
public class AInt {
	private final AtomicInteger ato = new AtomicInteger(0);
	public AInt() {
	}
	public AInt(int n) {
		while (!ato.compareAndSet(ato.get(), n));
	}
    // 取得,
	public int get() {
		return ato.get();
	}
    // セット.
	public void set(int n) {
		while (!ato.compareAndSet(ato.get(), n));
	}
    // int値を設定して前回の値を取得.
	public int put(int n) {
		int ret;
		while (!ato.compareAndSet((ret = ato.get()), n));
		return ret;
	}
    // 指定数の足し算.
	public int add(int no) {
		int n, r;
		while (!ato.compareAndSet((n = ato.get()), (r = n + no)));
		return r;
	}
    // 指定数の引き算.
	public int remove(int no) {
		int n, r;
		while (!ato.compareAndSet((n = ato.get()), (r = n - no)));
		return r;
	}
    // 1インクリメント = n ++;
	public int inc() {
		int n, r;
		while (!ato.compareAndSet((n = ato.get()), (r = n + 1)));
		return r;
	}
    // 1デクリメント = n --;
	public int dec() {
		int n, r;
		while (!ato.compareAndSet((n = ato.get()), (r = n - 1)));
		return r;
	}
    // 文字列で取得.
	public String toString() {
		return String.valueOf(ato.get());
	}
}
