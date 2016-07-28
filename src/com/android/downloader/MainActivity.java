package com.android.downloader;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import com.android.network.DownloadProgressListener;
import com.android.network.FileDownloader;

import android.app.Activity;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
	private EditText downloadpathText;
	private TextView resultView;
	private ProgressBar progressBar;

	/**
	 * 当Handler被创建会关联到创建它的当前线程的消息队列，该类用于往消息队列发送消息 消息队列中的消息由当前线程内部进行处理
	 * 使用Handler更新UI界面信息。
	 */
	private Handler handler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case 1:
				progressBar.setProgress(msg.getData().getInt("size"));
				float num = (float) progressBar.getProgress() / (float) progressBar.getMax();
				int result = (int) (num * 100);
				resultView.setText(result + "%");

				// 显示下载成功信息
				if (progressBar.getProgress() == progressBar.getMax()) {
					Toast.makeText(MainActivity.this, R.string.success, 1).show();
				}
				break;
				case 2:
					progressBar.setProgress(msg.arg1);
					float num1 = (float) progressBar.getProgress() / (float) progressBar.getMax();
					int result1 = (int) (num1 * 100);
					resultView.setText(result1 + "%");
					break;
			case -1:
				// 显示下载错误信息
				Toast.makeText(MainActivity.this, R.string.error, 1).show();
				break;
			}
		}
	};

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		downloadpathText = (EditText) this.findViewById(R.id.path);
		progressBar = (ProgressBar) this.findViewById(R.id.downloadbar);
		resultView = (TextView) this.findViewById(R.id.resultView);
		Button button = (Button) this.findViewById(R.id.button);
		Button buttonStop = (Button) this.findViewById(R.id.buttonStop);

		button.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				String path = downloadpathText.getText().toString();
				System.out.println(Environment.getExternalStorageState() + "------" + Environment.MEDIA_MOUNTED);

				if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
					// 开始下载文件
					download(path, Environment.getExternalStorageDirectory());
//					downloadSingle(path, Environment.getExternalStorageDirectory());
				} else {
					// 显示SDCard错误信息
					Toast.makeText(MainActivity.this, R.string.sdcarderror, 1).show();
				}
			}
		});
		buttonStop.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				if (null != loader) {
					loader.stop();
				}

			}
		});
	}

	/**
	 * 主线程(UI线程) 对于显示控件的界面更新只是由UI线程负责，如果是在非UI线程更新控件的属性值，更新后的显示界面不会反映到屏幕上
	 * 如果想让更新后的显示界面反映到屏幕上，需要用Handler设置。
	 * 
	 * @param fileUrlStr
	 * @param savedir
	 */
	FileDownloader loader;

	private void download(final String fileUrlStr, final File savedir) {
		new Thread(new Runnable() {
			@Override
			public void run() {
				// 开启3个线程进行下载
				loader = new FileDownloader(MainActivity.this, fileUrlStr, savedir, 3);
				progressBar.setMax(loader.getFileSize());// 设置进度条的最大刻度为文件的长度

				try {
					loader.download(new DownloadProgressListener() {
						@Override
						public void onDownloadSize(int size) {// 实时获知文件已经下载的数据长度
							Message msg = handler.obtainMessage();
							msg.what = 1;
							msg.getData().putInt("size", size);
							msg.sendToTarget();
						}
					});
				} catch (Exception e) {
					handler.obtainMessage(-1).sendToTarget();
				}
			}
		}).start();
	}

	private void downloadSingle(final String fileUrlStr, final File savedir) {

		new Thread(new Runnable() {

			@Override
			public void run() {
				// TODO Auto-generated method stub
				HttpURLConnection http;
				try {
					http = (HttpURLConnection) new URL(fileUrlStr).openConnection();
					http.setConnectTimeout(5 * 1000);
					http.setRequestMethod("GET");
					http.setRequestProperty("Accept",
							"image/gif, image/jpeg, image/pjpeg, image/pjpeg, application/x-shockwave-flash, application/xaml+xml, application/vnd.ms-xpsdocument, application/x-ms-xbap, application/x-ms-application, application/vnd.ms-excel, application/vnd.ms-powerpoint, application/msword, */*");
					http.setRequestProperty("Accept-Language", "zh-CN");
					// http.setRequestProperty("Referer", downUrl.toString());
					http.setRequestProperty("Charset", "UTF-8");
					http.setRequestProperty("Connection", "Keep-Alive");
					http.connect();
					progressBar.setMax(http.getContentLength());
					InputStream inStream = http.getInputStream();
					byte[] buffer = new byte[1024];
					int offset = 0;
					RandomAccessFile threadfile = new RandomAccessFile(new File(savedir,"youdao.apk"), "rwd");
					threadfile.seek(0);

					int len=0;
					while ((offset = inStream.read(buffer, 0, 1024)) != -1) {
						threadfile.write(buffer, 0, offset);
						len+=offset;
						Message message = handler.obtainMessage(2);
						message.arg1=len;
						message.sendToTarget();
					}
				} catch (MalformedURLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			}
		}).start();

	}

}