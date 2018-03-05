package com.android.tvremoteime;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.tvremoteime.server.RemoteServer;

import java.util.List;

public class MainActivity extends Activity implements View.OnClickListener {

    private ImageView qrCodeImage;
    private TextView addressView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        qrCodeImage = this.findViewById(R.id.ivQRCode);
        addressView = this.findViewById(R.id.tvAddress);

        refreshQRCode();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.btnUseIME:
                if(isEnableIME()){
                    Toast.makeText(getApplicationContext(), "太棒了，您已经激活启用了" + getString(R.string.app_name) +"输入法！", Toast.LENGTH_LONG).show();
                }else {
                    Intent intent = new Intent(Settings.ACTION_INPUT_METHOD_SETTINGS);
                    this.startActivityForResult(intent, 0);
                }
                break;
            case R.id.btnSetIME:
                if(!isEnableIME()) {
                    Toast.makeText(getApplicationContext(), "抱歉，请您先激活启用" + getString(R.string.app_name) +"输入法！", Toast.LENGTH_LONG).show();
                    Intent intent = new Intent(Settings.ACTION_INPUT_METHOD_SETTINGS);
                    this.startActivityForResult(intent, 0);
                    if(!isEnableIME()) return;
                }
                if(isDefaultIME()){
                    Toast.makeText(getApplicationContext(), "太棒了，" + getString(R.string.app_name) +"已是系统默认输入法！", Toast.LENGTH_LONG).show();
                }else{
                    ((InputMethodManager) getApplicationContext().getSystemService(Context.INPUT_METHOD_SERVICE)).showInputMethodPicker();
                }
                break;
            case R.id.btnStartService:
                startService(new Intent(IMEService.ACTION));
                Toast.makeText(getApplicationContext(), "服务已启动，请尝试访问控制端页面" , Toast.LENGTH_LONG).show();
                break;
        }
        refreshQRCode();
    }

    private boolean isEnableIME(){
        InputMethodManager imm = (InputMethodManager)this.getSystemService(Context.INPUT_METHOD_SERVICE);
        List<InputMethodInfo> inputs = imm.getEnabledInputMethodList();
        boolean flag = false;
        for(InputMethodInfo input : inputs){
            if(input.getPackageName().equals(this.getPackageName())){
                return true;
            }
        }
        return false;
    }

    private boolean isDefaultIME(){
        String defaultImme = Settings.Secure.getString(getContentResolver(),Settings.Secure.DEFAULT_INPUT_METHOD);

        if(defaultImme !=  null && defaultImme.startsWith(this.getPackageName())) {
            return true;
        }
        return false;
    }

    private void refreshQRCode(){
        String address = RemoteServer.getServerAddress(this);
        addressView.setText(address);
        qrCodeImage.setImageBitmap(QRCodeGen.generateBitmap(address, 150, 150));
    }
}
