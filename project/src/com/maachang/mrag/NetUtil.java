
package com.maachang.mrag;

/**
 * ネットワークユーティリティ.
 */
public final class NetUtil {
    private NetUtil() {}

	// ネットワーク初期定義.
	public static final void initNet() {
		// guiをoff.
		System.setProperty("java.awt.headless", "true");
		// IPV4で処理.
		System.setProperty("java.net.preferIPv4Stack", "true");
		// DNSキャッシュは300秒.
		System.setProperty("networkaddress.cache.ttl", "300");
		// DNS解決失敗した場合のキャッシュ保持しない.
		System.setProperty("networkaddress.cache.negative.ttl", "0");
	}

    
}