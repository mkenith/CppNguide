package com.example.cppnguide;
/*
import android.app.Activity;
import android.speech.tts.TextToSpeech;

import java.util.Locale;

class TextSpeech extends Activity implements  TextToSpeech.OnInitListener{
    private TextToSpeech textToSpeech;

    TextSpeech(TextToSpeech textToSpeech) {
        this.textToSpeech = textToSpeech;
    }

    public void Speak(String message){
        textToSpeech.speak(message, TextToSpeech.QUEUE_FLUSH, null,null);
    }
    public void onPause(){
        if(textToSpeech !=null){
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
        super.onPause();
    }

    @Override
    public void onInit(int i) {
        textToSpeech.setLanguage(Locale.US);
    }

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {
        super.onPointerCaptureChanged(hasCapture);
    }
}
*/