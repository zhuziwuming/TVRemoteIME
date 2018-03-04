package com.android.tvremoteime;

import android.content.Context;

import fi.iki.elonen.NanoHTTPD;
import player.XLVideoPlayActivity;

/**
 * Created by kingt on 2018/2/22.
 */

public class VideoPlayHelper {
    public static void playUrl(Context context, String url, boolean useSystem){
        if(useSystem) {

        }else {
            //内部播放
            XLVideoPlayActivity.intentTo(context, url, url);
        }
    }
}
