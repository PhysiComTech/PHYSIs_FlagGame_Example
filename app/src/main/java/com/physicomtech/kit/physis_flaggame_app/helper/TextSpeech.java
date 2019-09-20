package com.physicomtech.kit.physis_flaggame_app.helper;

import android.content.Context;
import android.os.Build;
import android.speech.tts.TextToSpeech;

import java.util.Locale;

import static android.speech.tts.TextToSpeech.ERROR;

public class TextSpeech {

    private TextToSpeech tts;

    public TextSpeech(Context context, final Locale locale, final float pitch, final float speechRate){
        tts = new TextToSpeech(context, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status != ERROR) {
                    tts.setLanguage(locale);                        // 출력 언어 설정
                    tts.setPitch(pitch);                            // 음성 톤 높이 설정
                    tts.setSpeechRate(speechRate);                  // 읽는 속도 설정
                }
            }
        });
    }

    public void speak(String state){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            tts.speak(state, TextToSpeech.QUEUE_FLUSH, null);
        } else {
            tts.speak(state, TextToSpeech.QUEUE_FLUSH, null, null);
        }
    }

    public void shutdown(){
        try{
            if(tts.isSpeaking()){
                tts.stop();
            }
        }catch (Exception e){
            e.getStackTrace();
        }
    }
}
