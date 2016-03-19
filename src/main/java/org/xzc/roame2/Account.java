package org.xzc.roame2;

import java.io.IOException;

import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.client.utils.HttpClientUtils;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.xzc.roame.SafeRunnable;
import org.xzc.roame.Utils;

/**
 * 封装了跟一个账号相关的信息
 * 
 * @author xzchaoo
 * 
 */
public class Account {
	public BasicCookieStore cs;
	public CloseableHttpClient hc;
	public String password;
	public String username;
	public boolean valid;

	public Account(String username, String password) {
		this.username = username;
		this.password = password;
		RequestConfig rc = RequestConfig.custom().setCookieSpec( CookieSpecs.NETSCAPE ).build();
		cs = new BasicCookieStore();
		hc = HttpClients.custom().setDefaultCookieStore( cs ).setDefaultRequestConfig( rc ).build();
	}

	/**
	 * 进行登录
	 */
	public void login() {
		Utils.safeCall( new SafeRunnable() {
			public Object run() throws Exception {
				// 往这个地址post就可以登陆
				String url = "http://www.roame.net/ajax.php?a=4098";
				HttpUriRequest post = RequestBuilder.post( url ).addParameter( "f", "1" )
						.addParameter( "m", username ).addParameter( "p", password )
						.addParameter( "r", "http://www.roame.net/" ).build();
				// 登录成功的话会有控制中心...
				CloseableHttpResponse res = null;
				// 结果会被重定向 你是拿不到结果的 所以需要调用hasLogined
				res = hc.execute( post );
				HttpClientUtils.closeQuietly( res );
				String index = "http://www.roame.net";
				res = hc.execute( RequestBuilder.get( index ).build() );
				valid = EntityUtils.toString( res.getEntity() ).contains( "控制中心" );
				HttpClientUtils.closeQuietly( res );
				return null;
			}
		} );
	}

	/**
	 * 关闭相应的hc
	 */
	void close() {
		if (hc != null)
			HttpClientUtils.closeQuietly( hc );
		hc = null;
	}

}