package org.xzc.roame2;

import org.junit.Test;

public class TestRoame {
	@Test
	public void test3() throws Exception {
		RoameWalker rw = new RoameWalker();
		rw.addAccount( "账号1", "密码1" );
		rw.addAccount( "账号2", "密码2" );
		//添加要下载的专辑的url
		rw.add( "http://www.roame.net/index/magical-shopping-arcade-abenobashi/images" );
		rw.add( "http://www.roame.net/index/parfait-tic/images" );
		rw.work();
		rw.waitDownload();
	}
	
}
