package org.xzc.roame2;

import java.util.Date;

/**
 * 用于表示一个下载线程的信息
 * 
 * @author xzchaoo
 * 
 */
public class ThreadInfo implements Cloneable {
	/**
	 * 线程当前的状态
	 */
	public State2 state2=State2.RUNNABLE;

	/**
	 * 总的任务数
	 */
	public int totalTaskCount;

	/**
	 * 成功的任务数
	 */
	public int successTaskCount;

	/**
	 * 失败的任务数
	 */
	public int failTaskCount;

	/**
	 * 跳过的任务数
	 */
	public int skipTaskCount;

	/**
	 * 启动时间
	 */
	public Date beginTime;

	/**
	 * 结束时间,可能为null如果线程还没有结束
	 */
	public Date endTime;

	@Override
	public ThreadInfo clone() throws CloneNotSupportedException {
		return (ThreadInfo) super.clone();
	}

}
