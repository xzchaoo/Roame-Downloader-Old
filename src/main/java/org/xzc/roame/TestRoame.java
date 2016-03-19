package org.xzc.roame;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URI;

import org.apache.commons.io.IOUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.Test;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlImage;
import com.gargoylesoftware.htmlunit.html.HtmlPage;

public class TestRoame {
	@Test
	public void testName() throws Exception {
		RoameWalker rw = new RoameWalker();
		boolean login = rw.login( "70862045@qq.com", "70862045" );
		if (!login) {
			System.out.println( "登陆失败" );
			return;
		}
		String baseUrl = "http://www.roame.net/index/saenai-heroine-no-sodatekata/images";
		rw.work1( baseUrl );
		try {
			//System.out.println( rw.getWc().getCookies( new URI(baseUrl).toURL() ) );
			rw.work2();
		} finally {
			rw.saveState();
		}
	}

	private void work1(RoameWalker rw) throws Exception {
		WebClient wc = rw.getWc();
		String baseUrl = "http://www.roame.net/index/saenai-heroine-no-sodatekata/images";
		int page = 2;
		String url = baseUrl + "/index_" + page + ".html";
		HtmlPage hp = wc.getPage( url );
		Document doc = Jsoup.parse( hp.asXml(), url );
		for (Element a : doc.select( ".fb a" )) {

			String imageUrl1 = getImageFinalUrl( wc, a.absUrl( "href" ) );
			method1( wc, imageUrl1 );
			break;
		}
	}

	private void method1(WebClient wc, String imageUrl) throws Exception {
		wc.getOptions().setJavaScriptEnabled( true );
		HtmlPage ihp = wc.getPage( imageUrl );
		HtmlImage hi = (HtmlImage) ihp.getElementById( "img" );
		hi.saveAs( new File( "roame/images/saenai-heroine-no-sodatekata/" + hi.getAltAttribute() ) );
		wc.getOptions().setJavaScriptEnabled( false );
	}

	private void method2(RoameWalker rw, String url) throws Exception {
		CloseableHttpClient hc = rw.createHttpClient();
		String filename = url.substring( url.lastIndexOf( '/' ) + 1 );
		HttpUriRequest get = RequestBuilder.get( url ).build();
		CloseableHttpResponse res = hc.execute( get );
		InputStream is = res.getEntity().getContent();

		FileOutputStream fos = new FileOutputStream( new File( RoameWalker.IMAGE_DIR, filename ) );
		IOUtils.copy( is, fos );
		fos.close();
		is.close();
		res.close();
		hc.close();
	}

	private void download(CloseableHttpClient hc, String url, String path) {
	}

	/**
	 * 通过形如http://www.roame.net/index/saenai-heroine-no-sodatekata/images/file-270877.
	 * html的页面获取最终的图片地址
	 * 
	 * @param baseUrl
	 * @return
	 */
	private String getImageFinalUrl(WebClient wc, String baseUrl) throws Exception {
		HtmlPage hp = wc.getPage( baseUrl );
		Document doc = Jsoup.parse( hp.asXml(), baseUrl );
		String src = doc.select( "#darlnks" ).parents().select( "> div > img" ).attr( "src" );
		src = src.substring( src.lastIndexOf( '/' ) + 1 );
		src = src.replaceAll( "\\.\\d+\\.", "." );
		return doc.select( "#darlnks a:eq(1)" ).attr( "abs:href" );// + src;
	}

	private String getImageFinalUrl2(WebClient wc, String baseUrl) throws Exception {
		HtmlPage hp = wc.getPage( baseUrl );
		Document doc = Jsoup.parse( hp.asXml(), baseUrl );
		String src = doc.select( "#darlnks" ).parents().select( "> div > img" ).attr( "src" );
		src = src.substring( src.lastIndexOf( '/' ) + 1 );
		src = src.replaceAll( "\\.\\d+\\.", "." );
		return doc.select( "#darlnks a:eq(1)" ).attr( "abs:href" ) + src;
	}

	// http://www.roame.net/index/saenai-heroine-no-sodatekata
	private void workWithIndexUrl(String url) {

	}
}
