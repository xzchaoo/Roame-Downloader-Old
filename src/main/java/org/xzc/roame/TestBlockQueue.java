package org.xzc.roame;

import java.util.Random;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;

public class TestBlockQueue {
	private static final Log LOG = LogFactory.getLog( TestBlockQueue.class );

	private LinkedBlockingQueue<Object> queue = new LinkedBlockingQueue<Object>();
	private boolean stop = false;
	private Random random=new Random();
	
	@Test
	public void testName() throws Exception {
		Thread t1 = new Thread( new Runnable() {
			public void run() {
				for (int i = 0; i < 10; ++i) {
					try {
						// 每一秒放一个东西进去
						Thread.sleep( 1000 );
						LOG.info( "放了一个进去" );
						queue.add( i );
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		} );
		Runnable r = new Runnable() {
			public void run() {
				while (!stop) {
					// 100毫秒拿一次
					try {
						Object obj = queue.poll( random.nextInt( 200 )+500, TimeUnit.MILLISECONDS );
						if (obj == null) {
							System.out.println( "没拿到 再试..." );
						} else {
							System.out.println( Thread.currentThread().getId() + "拿到" + obj );
						}
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		};
		Thread t2 = new Thread( r );
		Thread t3 = new Thread( r );
		t1.start();
		t2.start();
		t3.start();

		Thread.sleep( 11000 );
		stop = true;
		t1.join();
		t2.join();
		t3.join();
	}
}