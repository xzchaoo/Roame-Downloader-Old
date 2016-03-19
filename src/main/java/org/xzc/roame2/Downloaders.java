package org.xzc.roame2;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * 本质是一个消费者,从它的atList里不断的拿东西 它管理了一堆的AccountThread 将一个任务交给Downloaders,由Downloaders来决定如何下载该任务
 * 
 * 非线程安全
 * 
 * @author xzchaoo
 * 
 */
public class Downloaders {
	private static final Log LOG = LogFactory.getLog( Downloaders.class );

	/**
	 * 一个下载器有多个下载线程
	 */
	private List<AccountThread> atList = new ArrayList<AccountThread>();

	/**
	 * 下载结果回调
	 */
	private IDownloadCallback dc;

	/**
	 * 本实例的状态
	 */
	private State2 state2 = State2.RUNNABLE;

	public Downloaders(List<Account> list, IDownloadCallback dc) {
		this.dc = dc;
		for (Account a : list)
			atList.add( new AccountThread( a, dc ) );
	}

	/**
	 * 添加一个任务,任务立刻会被添加到所有的下载线程里
	 * 
	 * @param dw
	 */
	public void add(DownloadWork dw) {
		for (AccountThread at : atList)
			at.addToQueue( dw );
	}

	/**
	 * 
	 * @param a
	 */
	public void addAccountThread(Account a) {
		AccountThread at = new AccountThread( a, dc );
		atList.add( at );
		if (state2 == State2.RUNNING)
			at.start();
	}

	public DownloadersInfo getThreadsInfo() {
		DownloadersInfo ti = new DownloadersInfo();
		ti.total = atList.size();
		ti.active = 0;
		for (AccountThread at : atList) {
			if (at.getThreadInfo().state2 == State2.RUNNING)
				++ti.active;
		}
		return ti;
	}

	/**
	 * 启动所有的下载线程
	 */
	public void start() {
		if (state2 != State2.RUNNABLE) {
			LOG.warn( "Downloaders失败" + state2 );
			return;
		}
		state2 = State2.RUNNING;
		LOG.info( "Downloaders启动" );
		for (AccountThread at : atList)
			at.start();
	}

	/**
	 * 停止下载
	 * 
	 * @param stopNow
	 */
	public void stop(boolean stopNow) {
		for (AccountThread at : atList)
			at.setShouldStop( true );
		if (stopNow) {
			for (AccountThread at : atList)
				at.interrupt();
		}

	}

	public void stopIfEmpty() {
		for (AccountThread at : atList)
			at.setStopIfEmpty( true );
	}

}
