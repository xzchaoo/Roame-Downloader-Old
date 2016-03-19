package org.xzc.roame2;

/**
 * 单个下载 成功或失败 的回调
 * 
 * @author xzchaoo
 * 
 */
public interface IDownloadCallback {

	public void onDownloadWorkSuccess(AccountThread at, DownloadWork dw);

	public void onDownloadWorkFail(AccountThread at, DownloadWork dw);
}
