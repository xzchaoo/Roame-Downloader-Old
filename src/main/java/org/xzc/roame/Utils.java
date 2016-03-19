package org.xzc.roame;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Utils {
	/**
	 * windows下 目录不允许含有的字符 注意有些 需要转义
	 */
	public static final String FORBIDDEN_DIR_CHAR = "/\\:*?\"<>|";

	/**
	 * 把非法目录字符转成空格
	 * 
	 * @param title
	 * @return
	 */
	public static String normalizeTitle(String title) {
		if (title == null)
			return "";
		for (int i = 0; i < FORBIDDEN_DIR_CHAR.length(); ++i) {
			title = title.replace( FORBIDDEN_DIR_CHAR.charAt( i ), ' ' );
		}
		return title;
	}
	

	public static  void writeObject(final File file, final Object obj) {
		safeCall( new SafeRunnable() {
			@Override
			public Object run() throws Exception {
				FileOutputStream fos = new FileOutputStream( file );
				ObjectOutputStream oos = new ObjectOutputStream( fos );
				oos.writeObject( obj );
				oos.close();
				fos.close();
				return null;
			}
		} );
	}

	public static Object readObject(final File file) {
		if (!file.exists())
			return null;
		return safeCall( new SafeRunnable() {
			@Override
			public Object run() throws Exception {
				FileInputStream fis = new FileInputStream( file );
				ObjectInputStream ois = new ObjectInputStream( fis );
				Object obj = ois.readObject();
				ois.close();
				fis.close();
				return obj;
			}
		} );
	}
	
	public static Object safeCall(SafeRunnable sr) {
		try {
			return sr.run();
		} catch (Exception e) {
			throw ( e instanceof RuntimeException ) ? (RuntimeException) e : new RuntimeException(
					e );
		}
	}

	/**
	 * 先编译一下...
	 */
	private static Pattern MAX_PAGE_PATTERN = Pattern.compile( "共(\\d+)张" );

	/**
	 * 从给定的字符串中 抽出有几张
	 * 
	 * @param s
	 * @return
	 */
	public static int getCountFromTitle(String s) {
		Matcher m = MAX_PAGE_PATTERN.matcher( s );
		return m.find() ? Integer.parseInt( m.group( 1 ) ) : 0;
	}

}
