package org.xzc.roame;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.util.Cookie;

public class BaseWalker {
	protected WebClient wc;

	public BaseWalker() {
		wc = new WebClient();
	}

	protected Object safeCall(SafeRunnable sr) {
		try {
			return sr.run();
		} catch (Exception e) {
			throw ( e instanceof RuntimeException ) ? (RuntimeException) e : new RuntimeException(
					e );
		}
	}

	public CloseableHttpClient createHttpClient() {
		org.apache.http.cookie.Cookie[] httpClient = Cookie.toHttpClient( wc.getCookieManager()
				.getCookies() );
		// netscape类型的cookie
		RequestConfig rc = RequestConfig.custom().setCookieSpec( CookieSpecs.NETSCAPE ).build();
		BasicCookieStore cs = new BasicCookieStore();
		cs.addCookies( httpClient );
		CloseableHttpClient hc = HttpClients.custom().setDefaultCookieStore( cs )
				.setDefaultRequestConfig( rc ).build();
		return hc;
	}
}
