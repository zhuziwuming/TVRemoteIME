package com.android.tvremoteime;

import android.content.Intent;
import android.inputmethodservice.InputMethodService;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.InputConnection;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.tvremoteime.server.RemoteServer;
import com.android.tvremoteime.server.RemoteServerFileManager;

import java.io.IOException;


public class IMEService extends InputMethodService implements View.OnClickListener{
	public static String TAG = "TVRemoteIME";
	public static String ACTION = "com.android.tvremoteime";

	private boolean capsOn = false;
	private ImageButton btnCaps = null;
	private View focusedView = null;
	private RelativeLayout mInputView = null;

	private View helpDialog = null;
	private ImageView qrCodeImage = null;
	private TextView  addressView = null;

	private RemoteServer mServer = null;
	private LinearLayout qweLine = null;
	private LinearLayout asdLine = null;
	private LinearLayout zxcLine = null;

	private static final int SERVER_START_ERROR = 901;
	private static final int ERROR = 999;
	private static final int TOAST_MESSAGE = 1000;

	public static final int KEY_ACTION_PRESSED = 0;
	public static final int KEY_ACTION_DOWN = 1;
	public static final int KEY_ACTION_UP = 2;

	final Handler handler = new Handler(){
		@Override
		public void handleMessage(Message msg) {
			if(msg.what == SERVER_START_ERROR){
				Exception ex = (Exception)msg.obj;
				Toast.makeText(getApplicationContext(), "远程输入服务创建失败！错误信息：" + ex.getMessage(), Toast.LENGTH_LONG).show();
			}else if(msg.what == ERROR){
				Exception ex = (Exception)msg.obj;
				Toast.makeText(getApplicationContext(), "程序发生错误，错误信息：" + ex.getMessage(), Toast.LENGTH_LONG).show();
			}else if(msg.what == TOAST_MESSAGE){
				String text = (String)msg.obj;
				Toast.makeText(getApplicationContext(), text, Toast.LENGTH_LONG).show();
			}
		}
	};
	final Handler focusedClickHandler = new Handler();

	@Override
	public void onCreate() {
		super.onCreate();

		RemoteServerFileManager.resetBaseDir(this);
		startRemoteServer();

		//android.os.Debug.waitForDebugger();
	}

	@Override
    public View onCreateInputView() {
    	mInputView = (RelativeLayout)getLayoutInflater().inflate(R.layout.keyboard, null);

		capsOn = true;
		btnCaps = mInputView.findViewById(R.id.btnCaps);
		qweLine = mInputView.findViewById(R.id.qweLine);
		asdLine = mInputView.findViewById(R.id.asdLine);
		zxcLine = mInputView.findViewById(R.id.zxcLine);

		helpDialog = mInputView.findViewById(R.id.helpDialog);
		qrCodeImage = helpDialog.findViewById(R.id.ivQRCode);
		addressView = helpDialog.findViewById(R.id.tvAddress);

		toggleCapsState(true);

        return mInputView; 
    }

    private void showToastMsg(int what, Exception e){
		if(e == null){
			handler.sendEmptyMessage(what);
		}else{
			Message msg = new Message();
			msg.obj = e;
			msg.what = what;
			handler.sendMessage(msg);
		}
	}
	private void showToastMsg(String mssage){
		Message msg = new Message();
		msg.obj = mssage;
		msg.what = TOAST_MESSAGE;
		handler.sendMessage(msg);
	}

    private void startRemoteServer(){
		do {
			mServer = new RemoteServer(RemoteServer.serverPort, this);
			mServer.setDataReceiver(new RemoteServer.DataReceiver() {
				@Override
				public void onKeyEventReceived(String keyCode, int keyAction) {
					if(keyCode != null) {
						if("cls".equalsIgnoreCase(keyCode)){
							InputConnection ic = getCurrentInputConnection();
							if(ic != null) {
								ic.performContextMenuAction(android.R.id.selectAll);
								ic.commitText("", 1);
							}
						}else {
							final int kc = KeyEvent.keyCodeFromString(keyCode);
							if(kc != KeyEvent.KEYCODE_UNKNOWN){
								if(mInputView != null && KeyEventUtils.isKeyboardFocusEvent(kc) && mInputView.isShown()){
									if(keyAction == KEY_ACTION_PRESSED || keyAction == KEY_ACTION_DOWN) {
										handler.post(new Runnable() {
											@Override
											public void run() {
												if (!handleKeyboardFocusEvent(kc)) {
													sendKeyCode(kc);
												}
											}
										});
									}
								}
								else{
									long eventTime = SystemClock.uptimeMillis();
									InputConnection ic = getCurrentInputConnection();
									switch (keyAction) {
										case KEY_ACTION_PRESSED:
											sendKeyCode(kc);
											break;
										case KEY_ACTION_DOWN:
											if(ic != null) {
												ic.sendKeyEvent(new KeyEvent(eventTime, eventTime,
														KeyEvent.ACTION_DOWN, kc, 0, 0, KeyCharacterMap.VIRTUAL_KEYBOARD, 0,
														KeyEvent.FLAG_SOFT_KEYBOARD | KeyEvent.FLAG_KEEP_TOUCH_MODE));
											}
											break;
										case KEY_ACTION_UP:
											if(ic != null) {
												ic.sendKeyEvent(new KeyEvent(eventTime, eventTime,
													KeyEvent.ACTION_UP, kc, 0, 0, KeyCharacterMap.VIRTUAL_KEYBOARD, 0,
													KeyEvent.FLAG_SOFT_KEYBOARD | KeyEvent.FLAG_KEEP_TOUCH_MODE));
											}
											break;
									}
								}
							}
						}
					}
				}

				@Override
				public void onTextReceived(String text) {
					if (text != null) {
						commitText(text);
					}
				}
			});
			try {
				mServer.start();
				Log.i(TAG, "远程输入服务创建成功！port=" + RemoteServer.serverPort);
				break;
			}catch (IOException ex){
				Log.e(TAG, "建立远程输入HTTP服务时出错", ex);
				RemoteServer.serverPort ++;
				mServer.stop();
			}
		}while (RemoteServer.serverPort < 9999);
	}

	private boolean commitText(String text){
		InputConnection ic = getCurrentInputConnection();
		boolean flag = false;
		if (ic != null){
			Log.d(TAG, "commitText:" + text);
			if(text.length() > 1 && ic.beginBatchEdit()){
				flag = ic.commitText(text, 1);
				ic.endBatchEdit();
			}else{
				flag = ic.commitText(text, 1);
			}
		}
		return flag;
	}
	private void sendKeyCode(int keyCode){
		Log.d(TAG, "send-key-code:" + keyCode);
		if(keyCode == KeyEvent.KEYCODE_HOME){
			//拦截HOME键
			Intent i = new Intent(Intent.ACTION_MAIN);
			i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			i.addCategory(Intent.CATEGORY_HOME);
			this.startActivity(i);
		}else {
			sendDownUpKeyEvents(keyCode);
		}
	}
    
    public void onDestroy() {
		if (mServer != null && mServer.isStarting()){
            Log.i(TAG, "远程输入服务已停止！");
			mServer.stop();
		}
    	super.onDestroy();    	
    }

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if(handleKeyboardFocusEvent(keyCode)) return true;
		Log.d(TAG, "keydown-event:" + keyCode);

		//同步软键盘状态处理代码：不处理以下按键事件则有可能物理键盘字符输入与软键盘的大小写状态不同步
		if(keyCode == KeyEvent.KEYCODE_CAPS_LOCK) capsOn = !capsOn;
		if ((keyCode >= KeyEvent.KEYCODE_0 && keyCode <= KeyEvent.KEYCODE_9)) {
			if(commitText(String.valueOf(keyCode - KeyEvent.KEYCODE_0))) return true;
		} else if (keyCode >= KeyEvent.KEYCODE_A && keyCode <= KeyEvent.KEYCODE_Z) {
			if (commitText(String.valueOf((char) ((capsOn ? 65 : 97) + keyCode - KeyEvent.KEYCODE_A))))
				return true;
		}
		return super.onKeyDown(keyCode, event);
	}

	private boolean handleKeyboardFocusEvent(int keyCode){
		if(mInputView != null) {
			Log.d(TAG, "handleKeyboardFocusEvent:" + keyCode);
			switch (keyCode) {
				case KeyEvent.KEYCODE_DPAD_UP:
				case KeyEvent.KEYCODE_DPAD_DOWN:
				case KeyEvent.KEYCODE_DPAD_LEFT:
				case KeyEvent.KEYCODE_DPAD_RIGHT:
					if(mInputView.isShown()) {
						requestNextButtonFocus(keyCode);
						return true;
					}
					break;
				case KeyEvent.KEYCODE_ENTER:
				case KeyEvent.KEYCODE_DPAD_CENTER:
					if (mInputView.isShown() && focusedView != null) {
						clickButtonByKey(focusedView);
						return true;
					}
					break;
				case KeyEvent.KEYCODE_CAPS_LOCK:
					toggleCapsState(true);
					return true;
				case KeyEvent.KEYCODE_ESCAPE:
				case KeyEvent.KEYCODE_BACK:
					if (mInputView.isShown()){
						if(helpDialog != null && helpDialog.isShown()){
							helpDialog.setVisibility(View.GONE);
						}else {
							this.hideWindow();
						}
						return true;
					}
					break;
			}
		}
		return false;
	}

	private void requestNextButtonFocus(int keyCode){
		if(focusedView == null){
			focusedView =  ((LinearLayout)mInputView.getChildAt(0)).getChildAt(0);
		}else {
			LinearLayout container = (LinearLayout)focusedView.getParent();
			int rootInde = mInputView.indexOfChild(container);
			int index = container.indexOfChild(focusedView);
			boolean isLasted = container.getChildCount() == (index + 1);
			switch (keyCode) {
				case KeyEvent.KEYCODE_DPAD_UP:
					rootInde --;
					if(rootInde < 0) rootInde = mInputView.getChildCount() - 2;
					container = (LinearLayout)mInputView.getChildAt(rootInde);
					if(index >= container.getChildCount()) index = isLasted ? container.getChildCount() - 1 : 0;
					break;
				case KeyEvent.KEYCODE_DPAD_DOWN:
					rootInde ++;
					if(rootInde >= (mInputView.getChildCount() - 1)) rootInde = 0;
					container = (LinearLayout)mInputView.getChildAt(rootInde);
					if(index >= container.getChildCount()) index = isLasted ? container.getChildCount() - 1 :  0;
					break;
				case KeyEvent.KEYCODE_DPAD_LEFT:
					index --;
					if(index < 0){
						rootInde --;
						if(rootInde < 0) rootInde = mInputView.getChildCount() - 2;
						container = (LinearLayout)mInputView.getChildAt(rootInde);
						index = container.getChildCount() - 1;
					}
					break;
				case KeyEvent.KEYCODE_DPAD_RIGHT:
					index ++;
					if(index >= container.getChildCount()){
						rootInde ++;
						if(rootInde >= (mInputView.getChildCount() - 1)) rootInde = 0;
						container = (LinearLayout)mInputView.getChildAt(rootInde);
						index = 0;
					}
					break;
			}
			focusedView = container.getChildAt(index);
		}

		focusedView.requestFocus();
		focusedView.requestFocusFromTouch();
	}

	private void clickButtonByKey(View v){
		if(focusedView.getId() == R.id.btnCaps){
			focusedView.setBackgroundResource(capsOn ? R.drawable.key_pressed_on : R.drawable.key_pressed_off);
		}else{
			focusedView.setBackgroundResource(R.drawable.key_pressed);
		}
		clickButton(v, false);
		focusedClickHandler.postDelayed(new Runnable() {
			@Override
			public void run() {
				if(focusedView == btnCaps){
					focusedView.setBackgroundResource(capsOn ? R.drawable.key_on : R.drawable.key_off);
				}else{
					focusedView.setBackgroundResource(R.drawable.key);
				}
				focusedView.requestFocus();
			}
		}, 200);
	}
	private void clickButton(View v, boolean resetCapsButtonState){
		if(v instanceof Button){
			commitText(((Button) v).getText().toString());
		}else if(v instanceof ImageButton){
			switch (v.getId()){
				case R.id.btnEnter:
					sendKeyCode(KeyEvent.KEYCODE_ENTER);
					break;
				case R.id.btnSpace:
					sendKeyCode(KeyEvent.KEYCODE_SPACE);
					break;
				case R.id.btnDelete:
					sendKeyCode(KeyEvent.KEYCODE_DEL);
					break;
				case R.id.btnCaps:
					toggleCapsState(resetCapsButtonState);
					break;
				case R.id.btnHelp:
					showHelpDialog();
					break;
			}
		}
	}
	@Override
	public void onClick(View v) {
		clickButton(v, true);
		v.requestFocusFromTouch();
		focusedView = v;
	}

	private void toggleCapsState(boolean resetCapsButtonState){
		capsOn = !capsOn;
		if(resetCapsButtonState)
			btnCaps.setBackgroundResource(capsOn ? R.drawable.key_on : R.drawable.key_off);
		resetButtonChar(qweLine);
		resetButtonChar(asdLine);
		resetButtonChar(zxcLine);
	}
	private void resetButtonChar(LinearLayout layout){
		for(int i =0; i<layout.getChildCount(); i++){
			View v = layout.getChildAt(i);
			if(v instanceof Button){
				Button b = (Button)v;
				if(capsOn){
					b.setText(b.getText().toString().toUpperCase());
				}else{
					b.setText(b.getText().toString().toLowerCase());
				}
			}
		}
	}

	private void showHelpDialog(){
		if(mServer == null) return;

        if(addressView.getText().length() == 0) {
            String version = AppPackagesHelper.getCurrentPackageVersion(this);
            TextView title = helpDialog.findViewById(R.id.title);
            title.setText(title.getText() + " " + version);
            String address = mServer.getServerAddress();
            addressView.setText(address);
            qrCodeImage.setImageBitmap(QRCodeGen.generateBitmap(address, 300, 300));
        }

		helpDialog.setVisibility(View.VISIBLE);
	}

}
