package org.xzc.roame2;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.ParseException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.utils.HttpClientUtils;
import org.apache.http.impl.client.CloseableHttpClient;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

/**
 * 完全基于HttpClient版本的walker 一个账号同时只能有一个线程去下载,所以可以考虑多个号! 哈哈
 * 
 * @author xzchaoo
 * 
 */
public class RoameWalker implements IDownloadCallback {
	/**
	 * 根路径
	 */
	private static final String ROOT = "roame2";

	/**
	 * 保存图片的路径
	 */
	private static final String IMAGE = ROOT + File.separator + "images";

	/**
	 * 日志
	 */
	private static final Log LOG = LogFactory.getLog( RoameWalker.class );

	private static Object safeCall(SafeRunnable sr) {
		return Utils.safeCall( sr );
	}

	/**
	 * 用于保存用户添加的所有账号
	 */
	private List<Account> accounts = new ArrayList<Account>();

	/**
	 * 封装了多个线程的下载器
	 */
	private Downloaders ds;

	/**
	 * 主账号的hc,为了方便而用
	 */
	private CloseableHttpClient hc;

	/**
	 * 所有的roame索引地址
	 */
	private List<String> indexUrlList = new ArrayList<String>();

	/**
	 * 主账号 用于扫描下载地址,其本身也可以是一个用于下载的账号
	 */
	private Account master;

	/**
	 * 现在构造函数里没做什么事
	 */
	public RoameWalker() {
	}

	/**
	 * 添加一个索引地址,形如http://www.roame.net/index/fate-zero/images
	 * 
	 * @param s
	 */
	public void add(String s) {
		indexUrlList.add( s );
	}

	/**
	 * 添加一个账号
	 * 
	 * @param username
	 * @param password
	 */
	public void addAccount(String username, String password) {

		Account a = new Account( username, password );
		a.login();
		if (a.valid) {
			if (master == null) {
				master = a;
				hc = master.hc;
			}
			accounts.add( a );
		} else {
			HttpClientUtils.closeQuietly( a.hc );
			LOG.info( "密码错误: " + a.username );
		}

	}

	/**
	 * 不断阻塞直到下载完成,阻塞的时候会打印出一些当前信息
	 */
	public void waitDownload() {
		try {
			while (true) {
				DownloadersInfo ti = printInfo();
				if (ti.active == 0) {
					LOG.info( "活动线程=0,退出" );
					break;
				}
				Thread.sleep( 10000 );
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

	}

	/**
	 * 打印当前状态信息
	 */
	private DownloadersInfo printInfo() {
		DownloadersInfo ti = ds.getThreadsInfo();
		LOG.info( ti );
		return ti;
	}

	/**
	 * 本walker的状态
	 */
	private State2 state = State2.RUNNABLE;

	/**
	 * 根据indexUrl产生Project
	 * 
	 * @param indexUrl
	 * @return
	 * @throws ParseException
	 * @throws IOException
	 */
	private Project generateProject(String indexUrl) throws ParseException, IOException {
		String content = Utils.getToString( indexUrl, hc );
		Document doc = Jsoup.parse( content, indexUrl );
		Project p = new Project();
		p.url = indexUrl;
		p.title = doc.select( ".mtc > div > div > h1" ).text();
		p.title = Utils.normalizeTitle( p.title );
		p.total = Utils.getCountFromTitle( p.title );
		p.maxPage = p.total / 8 + ( p.total % 8 == 0 ? 0 : 1 );
		p.dir = new File( IMAGE, p.title );
		return p;
	}

	/**
	 * 开始下载
	 */
	public void work() {
		if (state == State2.RUNNING)
			return;
		state = State2.RUNNING;
		// 初始化账号
		init();
		// 启动下载器
		ds.start();

		safeCall( new SafeRunnable() {
			public Object run() throws Exception {
				// 用主账号处理每个给定的索引地址

				for (String index : indexUrlList) {
					Project p = generateProject( index );
					doProject( p );
				}
				// 从此以后如果下载线程的队列为空 那么那个下载线程就可以退出了
				// 因为不会再有新的任务了
				ds.stopIfEmpty();
				// 暂时不用了
				// es.shutdown();
				return null;
			}
		} );
		state = State2.STOPED;
	}

	/**
	 * 判断p是不是已经下载完毕了
	 */
	private static boolean isProjectFinished(Project p) {
		// 判断一下project是不是已经完成了
		File finished = new File( p.dir, "finished.txt" );
		return finished.exists();
	}

	/**
	 * 处理一个Project
	 * 
	 * @param p
	 * @throws IOException
	 * @throws ParseException
	 * @throws Exception
	 */
	private void doProject(Project p) throws ParseException, IOException {
		// 判断一下project是不是已经完成了
		if (isProjectFinished( p )) {
			LOG.info( String.format( "项目%s已经完成,跳过", p.title ) );
			return;
		}

		projectSuccessCount.put( p, 0 );

		// 创建出目录
		File dir = p.dir;
		if (!dir.exists()) {
			dir.mkdirs();
			LOG.info( "路径" + dir.getPath() + "不存在,创建它." );
		}

		// 下载每一页 8张图
		// 注意不要产生太多的url 否则下载的时间累加起来非常的长
		// 然后url还是动态变化的 所以时间太长的话 url会失效
		for (int page = 1; page <= p.maxPage; ++page) {
			String url = p.url + "/index_" + page + ".html";
			String content = Utils.getToString( url, hc );
			Document doc = Jsoup.parse( content, url );
			// 8张图
			for (Element a : doc.select( ".fb a" )) {
				// 找到每张图片的最终下载地址
				String imageUrl = getImageFinalUrl( a.absUrl( "href" ) );
				// 扔到下载队列里去
				mustDownloadImage( p, dir, imageUrl );
			}
		}
	}

	/**
	 * 根据形如 http://www.roame.net/index/fate-zero/images/file-267273.html 的url 获取图片最终的url
	 * 
	 * @param url
	 * @return
	 * @throws ClientProtocolException
	 * @throws IOException
	 */
	private String getImageFinalUrl(String url) throws ClientProtocolException, IOException {
		String html = Utils.getToString( url, hc );
		Document doc = Jsoup.parse( html, url );

		String src = doc.select( "#darlnks" ).parents().select( "> div > img" ).attr( "src" );

		src = src.substring( src.lastIndexOf( '/' ) + 1 );
		// 此时src形如ROAME_296935_B3CF00F1.512.jpg

		src = src.replaceAll( "\\.\\d+\\.", "." );
		// 此时src形如ROAME_296935_B3CF00F1.jpg

		// abs:href的结果形如
		// http://ios.roame.net/files/8UpF7joQnmL2Y5NBQ@8CNmhmpVHKtuyPIV8i188lXdN6FlZHtTsSilb54/
		return doc.select( "#darlnks a:eq(1)" ).attr( "abs:href" ) + src;
		// 最终结果形如
		// http://ios.roame.net/files/8UpF7joQnmL2Y5NBQ@8CNmhmpVHKtuyPIV8i188lXdN6FlZHtTsSilb54/ROAME_296935_B3CF00F1.jpg
	}

	/**
	 * 是否已经初始化
	 */
	private boolean hasInited = false;

	/**
	 * 初始化所有账号
	 */
	private void init() {
		if (hasInited)
			return;
		hasInited = true;
		ds = new Downloaders( accounts, this );
	}

	/**
	 * 根据p,dir,url下载一张图片到指定位置
	 * 
	 * @param p
	 * @param dir
	 * @param url
	 */
	private void mustDownloadImage(Project p, File dir, String url) {
		// 如果下载出错则遍历10个账号去下载
		// 扔到第一个账号的下载队列里
		// 并且扔一个回调函数
		// 当第一个账号执行完毕(无论成功失败)
		// 都调用回调函数然后判断下载成功与否
		// 如果失败就到第二个账号那里去下载
		// 可以考虑乱序 提速
		// 扔到a的下载队列里

		DownloadWork dw = new DownloadWork( p, url );

		while (works.size() >= accounts.size() * 5) {
			try {
				printInfo();
				Thread.sleep( 1000 );
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		workFailCount.put( dw, 0 );

		// 添加到下载器里
		ds.add( dw );
	}

	/**
	 * 停止下载
	 * 
	 * @param stopNow
	 */
	public void stop(boolean stopNow) {
		this.ds.stop( stopNow );
	}

	/**
	 * 每个任务下载了几页
	 */
	private Map<Project, Integer> projectSuccessCount = new HashMap<Project, Integer>();

	@Override
	public void onDownloadWorkSuccess(AccountThread at, DownloadWork dw) {
		works.remove( dw );
		workFailCount.remove( dw );
		Integer i = projectSuccessCount.get( dw.p );
		// 下载完成!
		if (++i == dw.p.total) {

			try {
				FileUtils.write( new File( dw.p.dir, "finished.txt" ), "ok!" );
			} catch (IOException e) {
				e.printStackTrace();
			}
			projectSuccessCount.remove( dw.p );
		} else {
			projectSuccessCount.put( dw.p, i );
		}
	}

	/**
	 * 当一次下载dw失败了 就统计一下它的失败次数,如果失败次数满了 那么就将它移除
	 */
	@Override
	public void onDownloadWorkFail(AccountThread at, DownloadWork dw) {
		Integer count = workFailCount.get( dw );
		if (count == null) {
			System.out.println( "一般不会为null啊!" );
			return;
		}
		++count;
		// 失败次数满了
		if (count == accounts.size()) {
			workFailCount.remove( dw );
			works.remove( dw );
		} else {
			workFailCount.put( dw, count );
		}
	}

	/**
	 * 所有已经提交的未完成的任务
	 */
	private Set<DownloadWork> works = new HashSet<DownloadWork>();

	/**
	 * 所有的已经提交的任务的失败次数
	 */
	private Map<DownloadWork, Integer> workFailCount = new HashMap<DownloadWork, Integer>();

}