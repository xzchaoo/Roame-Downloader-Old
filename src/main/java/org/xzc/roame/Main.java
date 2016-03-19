package org.xzc.roame;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.ListIterator;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.util.EntityUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.Test;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.WebClientOptions;
import com.gargoylesoftware.htmlunit.html.HtmlAnchor;
import com.gargoylesoftware.htmlunit.html.HtmlButtonInput;
import com.gargoylesoftware.htmlunit.html.HtmlDivision;
import com.gargoylesoftware.htmlunit.html.HtmlImage;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlPasswordInput;
import com.gargoylesoftware.htmlunit.html.HtmlTextInput;
import com.gargoylesoftware.htmlunit.util.Cookie;

public class Main {

	public static final class FileEntry {
		public String filekey;
		public String filename;

		public FileEntry() {
			super();
			// TODO Auto-generated constructor stub
		}

		public FileEntry(String filekey, String filename) {
			super();
			this.filekey = filekey;
			this.filename = filename;
		}

	}

	public class MyCookie extends BasicClientCookie {
		public MyCookie(String name, String value, String domain) {
			super( name, value );
			setDomain( domain );
			Date d = DateUtils.addYears( new Date(), 1 );
			setExpiryDate( d );
		}
	}

	private FileEntry getFileEntry(CloseableHttpClient hc, String url) throws Exception {
		HttpUriRequest get = RequestBuilder.get( url ).build();
		CloseableHttpResponse res = hc.execute( get );
		String content = EntityUtils.toString( res.getEntity() );
		Document doc = Jsoup.parse( content, url );
		res.close();

		Element darlnks = doc.select( "#darlnks" ).get( 0 );
		String href = darlnks.select( "a:eq(1)" ).attr( "href" );
		String filekey = href.substring( href.indexOf( "files/" ) + 6, href.length() - 1 );

		String src = darlnks.siblingElements().get( 1 ).select( "img" ).attr( "src" );
		String filename = src.substring( src.lastIndexOf( '/' ) + 1 );
		filename = filename.replaceFirst( "\\.\\d+\\.", "\\." );

		return new FileEntry( filekey, filename );
	}

	private void downloadPage(CloseableHttpClient hc, FileEntry fe) throws Exception {
		String purl = "http://ios.roame.net/files/" + fe.filekey + "/" + fe.filename;
		CloseableHttpResponse res = hc.execute( RequestBuilder.get( purl ).build() );
		InputStream is = res.getEntity().getContent();
		File file = new File( fe.filename );
		if (!file.exists()) {
			FileOutputStream fos = new FileOutputStream( file );
			IOUtils.copy( is, fos );
			fos.close();
		} else {
		}
		is.close();
		res.close();
	}

	private List<String> getPictureList(CloseableHttpClient hc, String baseUrl, int page)
			throws Exception {
		if (!baseUrl.endsWith( "/" ))
			baseUrl += "/";
		String url = baseUrl + "index_" + page + ".html";
		CloseableHttpResponse res = hc.execute( RequestBuilder.get( url ).build() );
		Document doc = Jsoup.parse( res.getEntity().getContent(), "utf-8", baseUrl );
		ListIterator<Element> iter = doc.select( ".fbi a" ).listIterator();
		List<String> ret = new ArrayList<String>();
		while (iter.hasNext()) {
			ret.add( iter.next().absUrl( "href" ) );
		}
		res.getEntity().getContent().close();
		res.close();
		return ret;
		// baseUrl=http://www.roame.net/index/fate-stay-night/images
		// /index_1.html
	}

	@Test
	public void test2() throws Exception {
		System.out.println( URLDecoder
				.decode( "\u767b\u5f55\u6807\u8bc6\u672a\u77e5", "iso-8859-1" ) );
	}

	@Test
	public void testImg() throws Exception {
		WebClient wc = new WebClient();
		WebClientOptions ops = wc.getOptions();
		ops.setJavaScriptEnabled( false );
		ops.setCssEnabled( false );
		ops.setThrowExceptionOnScriptError( false );
		

		Date d = new Date();
		d = DateUtils.addYears( d, 1 );
		String domain = "www.roame.net";
		wc.getCookieManager().addCookie( new Cookie( domain, "uid", "196796", "/", d, false ) );
		wc.getCookieManager().addCookie(
				new Cookie( domain, "upw", "a5c411efc4326ce9f91085bf9965cad2", "/", d, false ) );
		wc.getCookieManager().addCookie(
				new Cookie( domain, "cmd", "CaxXITtQ8P6I@wpSKpXDsZBynCDeLtmB", "/", d, false ) );

		// String url = "http://127.0.0.1/hc/index.php";
		// String url =
		// "http://ios.roame.net/files/7ahL91gqPjhN0BH@3KsV7rze48oekCCy95ml6HP2SP5P0CBF7s0mwXtgCH/";
		String url = "http://www.roame.net";
		HtmlPage hp = wc.getPage( url );
		System.out.println( hp.asXml().contains( "控制中心" ) );
		if(true)
			return;
		
		String baseUrl="http://www.roame.net/index/saenai-heroine-no-sodatekata/images/index.html";
		
		HtmlAnchor ha = (HtmlAnchor) hp.querySelectorAll( ".hdrui a" ).get( 0 );
		ha.click();

		HtmlDivision div = (HtmlDivision) hp.querySelectorAll( ".hbg > div" ).get( 1 );
		HtmlTextInput name = (HtmlTextInput) div.getElementsByAttribute( "input", "type", "text" )
				.get( 0 );
		name.setValueAttribute( "70862045@qq.com" );

		HtmlPasswordInput password = (HtmlPasswordInput) div.getElementsByAttribute( "input",
				"type", "password" ).get( 0 );
		password.setValueAttribute( "70862045" );

		HtmlButtonInput submit = (HtmlButtonInput) div.getElementsByAttribute( "input", "type",
				"button" ).get( 0 );
		// System.out.println( submit.asXml() );
		HtmlPage nhp = submit.click();
		// System.out.println(nhp.asXml());
		// div.getElementsByAttribute( "input", "type", "password" ).get( 0 );

		/*
		Iterator<DomElement> it = hp.getElementsByTagName( "input" ).iterator();
		while (it.hasNext()) {
			DomElement de = it.next();
			if ("登录".equals( de.getAttribute( "value" ) )) {
				System.out.println( de.asXml() );
			}
		}*/
		// HtmlImage hi = (HtmlImage) hp.getElementById( "img" );
		// System.out.println( hp.asXml().contains( "xzchaoo" ) );

		// hi.saveAs( new File( "saber.png" ) );
		// System.out.println( hp.getUrl() );
		System.out.println( nhp.asXml().contains( "控制中心" ) );
		// for (Cookie c : wc.getCookies( new URL( url ) )) {
		// System.out.println( c );
		//
		wc.close();

	}

	@Test
	public void test1() throws Exception {

		BasicCookieStore cs = new BasicCookieStore();
		cs.addCookie( new MyCookie( "uid", "196796", ".roame.net" ) );
		cs.addCookie( new MyCookie( "upw", "a5c411efc4326ce9f91085bf9965cad2", ".roame.net" ) );
		cs.addCookie( new MyCookie( "cmd", "ZG82jk6PNxm941FUaFovv52us64ZvGr", ".roame.net" ) );

		// http://ios.roame.net/files/7ahL91gqPjhN0BH@3KsV7rze48oekCCy95ml6HP2SP5P0CBF7s0mwXtgCH/

		RequestConfig rc = RequestConfig.custom().setCookieSpec( CookieSpecs.NETSCAPE ).build();
		CloseableHttpClient hc = HttpClients.custom().setDefaultCookieStore( cs )
				.setDefaultRequestConfig( rc ).build();

		// String baseUrl = "http://www.roame.net/index/fate-stay-night/images";
		// List<String> list = getPictureList( hc, baseUrl, 1 );
		// for (String url : list) {
		// FileEntry fe = getFileEntry( hc, url );
		// downloadPage( hc, fe );
		// }
		// String url = "http://www.roame.net/index/fate-stay-night/images/file-269290.html";
		// FileEntry fe = getFileEntry( hc, url );
		// downloadPage( hc, fe );
		// testGetElementById( hc );
		// HttpUriRequest get =
		// RequestBuilder.get("http://ios.roame.net/files/7ahL91gqPjhN0BH@3KsV7rze48oekCCy95ml6HP2SP5P0CBF7s0mwXtgCH/").build();
		// CloseableHttpResponse res = hc.execute( get );
	}

	@Test
	public void testGetElementById() throws Exception {
		String url = "http://ios.roame.net/files/2Dad0NntU6Ub3XPXv9cW5NmdV9yPcaiv2Sq9BNSCG6ZUQt2HXpPDrhHuC/";
		WebClient wc = new WebClient();
		WebClientOptions ops = wc.getOptions();
		ops.setJavaScriptEnabled( true );
		ops.setCssEnabled( false );
		ops.setThrowExceptionOnScriptError( false );
		ops.setTimeout( 10000 );
		HtmlPage hp = wc.getPage( url );
		HtmlImage hi = (HtmlImage) hp.getElementById( "img" );
		hi.saveAs( new File( "test.png" ) );
		System.out.println( wc.getCookies( new URL( url ) ) );
		wc.close();
	}

}
