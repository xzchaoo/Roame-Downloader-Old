package org.xzc.roame2;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.SocketException;
import java.rmi.server.SocketSecurityException;
import java.util.Date;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.client.utils.HttpClientUtils;
import org.apache.http.impl.client.CloseableHttpClient;

/**
 * 跟一个账号绑定的下载线程 更确切的说是跟一个HttpClient绑定的线程 是一个消费者 不断从queue拿任务来下载
 * 
 * @author xzchaoo
 * 
 */
public class AccountThread extends Thread {
	private static final Log LOG = LogFactory.getLog( AccountThread.class );
	private Account a;
	private CloseableHttpClient hc;
	private LinkedBlockingQueue<DownloadWork> queue = new LinkedBlockingQueue<DownloadWork>();
	private ThreadInfo threadInfo;

	private IDownloadCallback downloadCallback;

	public AccountThread(Account a, IDownloadCallback downloadCallback) {
		this.a = a;
		this.hc = a.hc;
		threadInfo = new ThreadInfo();
		this.downloadCallback = downloadCallback;
	}

	public void run() {
		threadInfo.beginTime = new Date();
		threadInfo.state2 = State2.RUNNING;
		LOG.info( "tid=" + getId() + "开始工作" );

		int retryCount = 0;
		boolean retry = false;
		DownloadWork dw = null;
		// 当不停的时候做...
		while (!shouldStop) {
			try {

				// 重试5次 包括第1次 总计6次下载
				if (retry) {
					retry = false;
					if (++retryCount < 5)
						LOG.info( String.format( "tid=%d,%s,进行第%d次重试.", getId(), dw.filename,
								retryCount ) );
					else {
						LOG.info( String.format( "tid=%d,%s,超过重试次数,下载失败.", getId(), dw.filename ) );
						continue;
					}
				} else {
					dw = queue.poll( 1, TimeUnit.SECONDS );
					retryCount = 0;
				}

				if (dw != null) {

					// 如果别人已经成功下载了 那么我就不做了
					if (dw.success) {
						++threadInfo.skipTaskCount;
						continue;
					}

					if (dw.lock.tryLock()) {
						try {
							doDownload( dw );
							LOG.info( "tid=" + getId() + " 下载" + dw.filename + " success="
									+ dw.success );
							if (dw.success) {
								downloadCallback.onDownloadWorkSuccess( this, dw );
								++threadInfo.successTaskCount;
							} else {
								downloadCallback.onDownloadWorkFail( this, dw );
								++threadInfo.failTaskCount;
							}
						} finally {
							dw.lock.unlock();
						}
					} else {
						// 没拿到锁扔回队列
						queue.offer( dw );
						// 这里是要睡觉的,否则会死循环
						Thread.sleep( 1000 );
					}
				} else {
					// 我们规定当队列为空的时候线程就结束
					if (stopIfEmpty)
						break;
					else {
						LOG.info( "tid=" + getId() + "的下载任务队列为空,但是不能退出线程." );
						// 不用睡觉因为poll会睡觉...
						// Thread.sleep( 1000 );
					}
				}
				// 遇到异常的话仅仅只是报告一下
				// 一般是遇到SocketException
			} catch (RuntimeException e) {
				if (e.getCause() != null && e.getCause() instanceof SocketException) {
					retry = true;
					// 重试
				} else {
					downloadCallback.onDownloadWorkFail( this, dw );
					++threadInfo.failTaskCount;
					e.printStackTrace();
				}
			} catch (InterruptedException e) {
				// 被打断 那就走人了 一般是人为主动打断的
				e.printStackTrace();
				break;
			} catch (Exception e) {
				downloadCallback.onDownloadWorkFail( this, dw );
				++threadInfo.failTaskCount;
				e.printStackTrace();
			} finally {
			}
		}
		threadInfo.state2 = State2.STOPED;
		LOG.info( "tid=" + getId() + "结束" );
	}

	/**
	 * 根据dw进行下载,如果下载成功会自动将dw.success设置为true
	 * 
	 * @param dw
	 */
	private void doDownload(final DownloadWork dw) {
		final String url = dw.url;
		final File dir = dw.p.dir;
		final String filename = dw.filename;
		final File file = new File( dir, filename );
		
		if (file.exists()) {
			LOG.info( String.format( "文件%s,已经存在,跳过.", filename ) );
			dw.success = true;
			return;
		}
		
		Utils.safeCall( new SafeRunnable() {
			public Object run() throws Exception {
				CloseableHttpResponse res = hc.execute( RequestBuilder.get( url ).build() );
				InputStream is = res.getEntity().getContent();
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				LOG.info( "tid=" + getId() + " 正在下载" + dw.filename );
				long begin = System.currentTimeMillis();
				// 如果线程太多经常会遇到SocketException: Connection reset
				IOUtils.copy( is, baos );
				long end = System.currentTimeMillis();
				byte[] data = baos.toByteArray();
				if (data.length == 0) {
					return null;
				}
				FileOutputStream fos = new FileOutputStream( file );
				fos.write( baos.toByteArray() );
				IOUtils.closeQuietly( fos );
				IOUtils.closeQuietly( baos );
				IOUtils.closeQuietly( is );
				HttpClientUtils.closeQuietly( res );

				long time = ( end - begin ) / 1000;
				long length = file.length() / 1024;
				long avgTime = time == 0 ? -1 : length / time;
				dw.success = true;
				LOG.info( String.format( "%s下载完毕 大小=%dkb 耗时=%d秒 平均速度=%dkb/s", file.getName(),
						length, time, avgTime ) );
				return null;
			}
		} );

	}

	/**
	 * 添加到下载队列
	 * 
	 * @param dw
	 */
	public synchronized void addToQueue(DownloadWork dw) {
		queue.add( dw );
		threadInfo.totalTaskCount = queue.size();
	}

	/**
	 * 如果下载队列为空的时候,是否停止该线程?还是继续等待新的任务
	 */
	private boolean stopIfEmpty;

	/**
	 * 下载线程是否被请求停止
	 */
	private boolean shouldStop;

	public boolean isStopIfEmpty() {
		return stopIfEmpty;
	}

	public void setStopIfEmpty(boolean stopIfEmpty) {
		this.stopIfEmpty = stopIfEmpty;
	}

	public ThreadInfo getThreadInfo() {
		return threadInfo;
	}

	public boolean isShouldStop() {
		return shouldStop;
	}

	public void setShouldStop(boolean shouldStop) {
		this.shouldStop = shouldStop;
	}

}