package org.xzc.roame;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.client.utils.HttpClientUtils;
import org.apache.http.impl.client.CloseableHttpClient;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import com.gargoylesoftware.htmlunit.CookieManager;
import com.gargoylesoftware.htmlunit.HttpMethod;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.WebClientOptions;
import com.gargoylesoftware.htmlunit.WebRequest;
import com.gargoylesoftware.htmlunit.html.DomNode;
import com.gargoylesoftware.htmlunit.html.HtmlImage;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.util.NameValuePair;

/**
 * 用于从www.roame.net进行下载对于每个给定的账号,只能单线程 因此如果你想多下的话 需要开多个账号
 * 
 * @author xzchaoo
 * 
 */
public class RoameWalker extends BaseWalker {

	private static final Log log = LogFactory.getLog( RoameWalker.class );
	private static final File ROOT = new File( "roame" );
	private static final File FINISHED = new File( ROOT, "finished" );

	public static final File IMAGE_DIR = new File( ROOT, "images" );
	private static final File COOKIE_FILE = new File( ROOT, "cookie" );

	/**
	 * 已下载完成的地址们 以页为单位 用 Project.url-page 的格式组成
	 */
	private Set<String> finished;

	public RoameWalker() {
		WebClientOptions ops = wc.getOptions();

		// 尝试恢复保存过的cookie
		CookieManager cm = null;// (CookieManager) Utils.readObject( COOKIE_FILE );
		if (cm != null) {
			wc.setCookieManager( cm );
		} else {
			log.info( "CookieManager不存在,使用默认实例." );
		}

		// 默认关掉js引擎
		ops.setJavaScriptEnabled( false );
		ops.setCssEnabled( false );
		ops.setThrowExceptionOnScriptError( false );

		// 初始化 已完成的项目-页
		finished = (Set<String>) Utils.readObject( FINISHED );
		if (finished == null)
			finished = new HashSet<String>();

		/*
				Date d = new Date();
				d = DateUtils.addYears( d, 1 );
				String domain = "www.roame.net";
				wc.getCookieManager().addCookie( new Cookie( domain, "uid", "196796", "/", d, false ) );
				wc.getCookieManager().addCookie(
						new Cookie( domain, "upw", "a5c411efc4326ce9f91085bf9965cad2", "/", d, false ) );
				wc.getCookieManager().addCookie(
						new Cookie( domain, "cmd", "CaxXITtQ8P6I@wpSKpXDsZBynCDeLtmB", "/", d, false ) );
						*/
	}

	/**
	 * 进行登录 好在这个网站不需要验证码 哈哈
	 * 
	 * @param username
	 * @param password
	 * @return 登录成功与否
	 */
	public boolean login(final String username, final String password) {
		if (hasLogined()) {
			log.info( "你已经登陆了,因为有Cookie!" );
			return true;
		}
		return (Boolean) safeCall( new SafeRunnable() {
			@Override
			public Object run() throws Exception {
				// 向这个地址发送post请求即可登录
				String url = "http://www.roame.net/ajax.php?a=4098";
				WebRequest wr = new WebRequest( new URL( url ), HttpMethod.POST );
				List<NameValuePair> params = new ArrayList<NameValuePair>();

				params.add( new NameValuePair( "f", "1" ) );
				params.add( new NameValuePair( "m", username ) );
				params.add( new NameValuePair( "p", password ) );
				params.add( new NameValuePair( "r", "http://www.roame.net/" ) );
				wr.setRequestParameters( params );
				HtmlPage hp = wc.getPage( wr );
				// 登录成功的话会有控制中心...
				return hp.asXml().contains( "控制中心" );
			}
		} );
	}

	/**
	 * 检查是否已经登陆
	 * 
	 * @return
	 */
	public boolean hasLogined() {
		final String url = "http://www.roame.net";
		return (Boolean) safeCall( new SafeRunnable() {
			@Override
			public Object run() throws Exception {
				HtmlPage hp = wc.getPage( url );
				return hp.asXml().contains( "控制中心" );
			}
		} );
	}

	/**
	 * 保存当前的状态 以便以后进行恢复
	 */
	public void saveState() {
		Utils.writeObject( COOKIE_FILE, wc.getCookieManager() );
		Utils.writeObject( FINISHED, finished );
	}

	/**
	 * 暂时没用了
	 * 
	 * @return
	 */
	@Deprecated
	public WebClient getWc() {
		return wc;
	}

	/**
	 * 用于描述一个下载项目
	 * 
	 * @author xzchaoo
	 * 
	 */
	public class Project {
		/**
		 * 形如http://www.roame.net/index/saenai-heroine-no-sodatekata/images
		 */
		public String url;
		/**
		 * title的话是根据url里的 title标签 抽出来的
		 */
		public String title;
		/**
		 * 这个是根据 图片总数 和 每页8张图片 算出来的
		 */
		int maxPage;

		@Override
		public String toString() {
			return "Project [url=" + url + ", title=" + title + ", maxPage=" + maxPage + "]";
		}
	}

	private LinkedBlockingQueue<Project> queue1 = new LinkedBlockingQueue<Project>();

	public void work1(final String baseUrl) {
		safeCall( new SafeRunnable() {
			@Override
			public Object run() throws Exception {
				HtmlPage hp = wc.getPage( baseUrl );
				DomNode dn = hp.querySelector( ".mtc > div > div > h1" );
				String title = dn.asText();
				Project p = new Project();
				int count = Utils.getCountFromTitle( title );

				p.url = baseUrl;

				p.title = Utils.normalizeTitle( title );

				p.maxPage = count / 8 + ( count % 8 == 0 ? 0 : 1 );
				queue1.add( p );
				return null;
			}
		} );
	}

	/**
	 * 第二步
	 */
	public void work2() {
		// 开启若干线程去下载 使用HttpClient 注意每个线程自己有一个HttpClient
		safeCall( new SafeRunnable() {
			@Override
			public Object run() throws Exception {
				for (Project p : queue1) {
					downloadProject( p );
				}
				return null;
			}
		} );
	}

	private String generateFinishedKey(Project p, int page) {
		return p.url + "-" + page;
	}

	/**
	 * 去下载一个Project
	 * 
	 * @param p
	 * @throws Exception
	 */
	private void downloadProject(final Project p) {
		log.info( "正在下载: " + p );
		safeCall( new SafeRunnable() {
			@Override
			public Object run() throws Exception {
				// 保证目录存在 同一个Project共享功同一个目录 所以... 在这里进行
				File dir = new File( IMAGE_DIR, p.title );
				if (!dir.exists())
					dir.mkdirs();

				// 下载每一页 每一页有8张图
				for (int page = 1; page <= p.maxPage; ++page) {
					处理页( p, page );
				}
				return null;
			}
		} );
	}

	/**
	 * 下载一个Project的一个页 8张图
	 * 
	 * @param p
	 * @param page
	 * @throws Exception
	 */
	private void 处理页(Project p, int page) throws Exception {
		log.info( String.format( "正在下载: %s 第%d页", p.title, page ) );
		String key = generateFinishedKey( p, page );
		if (finished.contains( key )) {
			log.info( String.format( "key=%s, %s-%d 已经存在,跳过", key, p.title, page ) );
			return;
		}
		// 拼凑出页的地址
		String url = p.url + "/index_" + page + ".html";
		HtmlPage hp = wc.getPage( url );
		Document doc = Jsoup.parse( hp.asXml(), url );
		// 八张图 可以通过 表达式 .fb a 定位
		for (Element a : doc.select( ".fb a" )) {
			// 找到最终下载地址
			String imageUrl1 = getImageFinalUrl2( a.absUrl( "href" ) );
			downloadImageWithFinalUrl( p, imageUrl1 );
		}

		finished.add( key );
		log.info( String.format( "第%d页下载完成", page ) );
	}

	private String getImageFinalUrl1(String baseUrl) throws Exception {
		HtmlPage hp = wc.getPage( baseUrl );
		Document doc = Jsoup.parse( hp.asXml(), baseUrl );
		String src = doc.select( "#darlnks" ).parents().select( "> div > img" ).attr( "src" );
		src = src.substring( src.lastIndexOf( '/' ) + 1 );
		src = src.replaceAll( "\\.\\d+\\.", "." );
		return doc.select( "#darlnks a:eq(1)" ).attr( "abs:href" );// + src;
	}

	/**
	 * 注意这里的url不同
	 * 
	 * @param p
	 * @param url
	 * @throws Exception
	 */
	private void downloadImageWithFinalUrl1(Project p, String url) throws Exception {
		wc.getOptions().setJavaScriptEnabled( true );
		HtmlPage ihp = wc.getPage( url );
		HtmlImage hi = (HtmlImage) ihp.getElementById( "img" );
		// 获得图片的名字 一般取最后一个 '/' 后面的字符串就行
		String filename = hi.getAltAttribute();

		File file = new File( RoameWalker.IMAGE_DIR, p.title + "/" + filename );

		hi.saveAs( file );
		wc.getOptions().setJavaScriptEnabled( false );
	}

	/**
	 * 根据最后的图片URL进行下载
	 * 
	 * @param p
	 * @param url
	 * @throws Exception
	 */
	private void downloadImageWithFinalUrl(Project p, String url) throws Exception {
		log.info( "正在下载" + url );
		// 获得图片的名字 一般取最后一个 '/' 后面的字符串就行
		String filename = url.substring( url.lastIndexOf( '/' ) + 1 );

		File file = new File( RoameWalker.IMAGE_DIR, p.title + "/" + filename );
		if (file.exists() && file.length() == 0)
			file.delete();

		CloseableHttpClient hc = getHttpClient();
		HttpUriRequest get = RequestBuilder.get( url ).build();
		CloseableHttpResponse res = hc.execute( get );
		InputStream is = res.getEntity().getContent();
		long length = res.getEntity().getContentLength();

		// 文件存在 虽然无法判断文件是否完整 但是还是先选择跳过
		if (file.exists()) {
			log.info( String.format( "%s已经存在,length=%d,content-length=%d,跳过", file.getName(),
					file.length(), length ) );
			return;
		}
		FileOutputStream fos = null;
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		// 这步可能非常的耗时 自身网络因素 和 对方服务器限速
		boolean success = false;
		try {
			long begin = System.currentTimeMillis();
			IOUtils.copy( is, baos );
			byte[] data = baos.toByteArray();
			if (data.length == 0) {
				log.info( "遇到data.length=0" );
			} else {
				fos = new FileOutputStream( file );
				long end = System.currentTimeMillis();
				fos.write( baos.toByteArray() );
				success = true;
				log.info( String.format( "%s 大小=%dkb 耗时%d秒", filename, length / 1024,
						( end - begin ) / 1000 ) );
			}
		} finally {
			IOUtils.closeQuietly( baos );
			IOUtils.closeQuietly( fos );
			IOUtils.closeQuietly( is );
			HttpClientUtils.closeQuietly( res );
			HttpClientUtils.closeQuietly( hc );
			if (!success)
				file.delete();
		}
	}

	private CloseableHttpClient getHttpClient() {
		return createHttpClient();
	}

	/**
	 * 根据形如http://www.roame.net/index/saenai-heroine-no-sodatekata/images/file-270877.
	 * html的url找出图片的最终地址
	 * 
	 * @param wc
	 * @param baseUrl
	 * @return
	 * @throws Exception
	 */
	private String getImageFinalUrl2(String baseUrl) throws Exception {
		HtmlPage hp = wc.getPage( baseUrl );
		Document doc = Jsoup.parse( hp.asXml(), baseUrl );

		String src = doc.select( "#darlnks" ).parents().select( "> div > img" ).attr( "src" );
		src = src.substring( src.lastIndexOf( '/' ) + 1 );
		src = src.replaceAll( "\\.\\d+\\.", "." );
		// src形如ROAME_296935_B3CF00F1.512.jpg

		// abs:href的结果形如
		// http://ios.roame.net/files/8UpF7joQnmL2Y5NBQ@8CNmhmpVHKtuyPIV8i188lXdN6FlZHtTsSilb54/
		return doc.select( "#darlnks a:eq(1)" ).attr( "abs:href" ) + src;
	}

}
