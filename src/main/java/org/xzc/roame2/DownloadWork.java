package org.xzc.roame2;

import java.util.concurrent.locks.ReentrantLock;

/**
 * 用于描述一个下载任务,即一个图片的下载而已
 * 
 * @author xzchaoo
 * 
 */
public class DownloadWork {
	/**
	 * 跟该任务相关的Project
	 */
	public Project p;
	/**
	 * 下载保存时的名字
	 */
	public String filename;
	/**
	 * 图片的url
	 */
	public String url;
	/**
	 * 表示是否下载成功
	 */
	boolean success;
	/**
	 * 锁,同一时刻只能有一个工作线程在下载这个图片
	 */
	public ReentrantLock lock = new ReentrantLock();

	public DownloadWork(Project p, String url) {
		this.p = p;
		this.url = url;
		this.filename = url.substring( url.lastIndexOf( '/' ) + 1 );
	}
}