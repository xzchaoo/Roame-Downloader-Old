package org.xzc.roame;

import java.io.FileOutputStream;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.junit.Test;

import com.gargoylesoftware.htmlunit.UnexpectedPage;
import com.gargoylesoftware.htmlunit.WebClient;

public class TestReadAndWriteObject {
	@Test
	public void testName() throws Exception {
		WebClient wc = new WebClient();
		UnexpectedPage p = wc
				.getPage( "http://fuss10.elemecdn.com/e/51/07bdcb29f5ad2fd3845da4ea02cf1png.png" );
		InputStream is = p.getInputStream();
		FileOutputStream fos = new FileOutputStream( "test1.png" );
		IOUtils.copy( is, fos );
		IOUtils.closeQuietly( fos );
		IOUtils.closeQuietly( is );
		wc.close();
	}
}
