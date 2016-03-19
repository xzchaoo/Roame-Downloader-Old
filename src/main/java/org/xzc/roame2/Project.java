package org.xzc.roame2;

import java.io.File;

/**
 * 用于描述一个Project 这个Project并没有对应的图片的信息 因为可能会相当多 一个Project可能有上千张图片 这不太好...
 * 
 * @author xzchaoo
 * 
 */
public class Project {
	public File dir;
	public int maxPage;
	public String title;
	public String url;
	public int total;
}