package st.pushupcounter.view.util;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.speech.tts.TextToSpeech;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.RawRes;

/**
 * @author : "Line'R"
 * @mailto : serinity320@mail.com
 * @created : 27.01.2023, пятница
 **/
public enum AudioRoute {
    HEADSET(AudioManager.STREAM_MUSIC, AudioManager.MODE_NORMAL),
    SPEAKER(AudioManager.STREAM_VOICE_CALL, AudioManager.MODE_IN_COMMUNICATION);
    private final int streamType;
    private final int modeType;

    AudioRoute(int streamType, int modeType) {
        this.streamType = streamType;
        this.modeType = modeType;
    }

    /**
     * Update audio route for audio manager, used for enabling or disabling speakers
     *
     * @param audioManager value
     */
    public void update(AudioManager audioManager) {
        audioManager.setMode(modeType);
        audioManager.setSpeakerphoneOn(this == SPEAKER);
    }

    /**
     * Update audio route for mediaPlayer, used for switching stream when audio manager changes mode.
     * This method prevents system auto change mode to NORMAL
     * This method throws IllegalStateException if called after mediaPlayer.prepare();
     *
     * @param mediaPlayer value
     */
    public void update(MediaPlayer mediaPlayer) {
        mediaPlayer.setAudioAttributes(new AudioAttributes.Builder().setLegacyStreamType(streamType).build());
    }

    /**
     * Recreate mediaPlayer, with specified route.
     * This method prevents system auto change mode to NORMAL or creating new instance of media player
     *
     * @param mediaPlayer value
     */
    public MediaPlayer recreate(Context context, MediaPlayer mediaPlayer, @RawRes int resource) {
        try {
            if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
                mediaPlayer.release();
            }
            mediaPlayer = new MediaPlayer();
            AssetFileDescriptor assetFileDescriptor = context.getResources().openRawResourceFd(resource);
            mediaPlayer.setDataSource(assetFileDescriptor.getFileDescriptor(), assetFileDescriptor.getStartOffset(), assetFileDescriptor.getLength());
            assetFileDescriptor.close();
            mediaPlayer.setAudioAttributes(new AudioAttributes.Builder().setLegacyStreamType(streamType).build());
            mediaPlayer.prepare();
            return mediaPlayer;
        } catch (Exception e) {
            Log.e("AudioRoute", "Unable to prepare media player: ", e);
            return new MediaPlayer();
        }
    }

    /**
     * Update audio route for textToSpeech, used for switching stream when audio manager changes mode.
     * This method prevents system auto change mode to NORMAL
     *
     * @param textToSpeech value
     */
    public void update(TextToSpeech textToSpeech) {
        textToSpeech.setAudioAttributes(new AudioAttributes.Builder().setLegacyStreamType(streamType).build());
    }

    @NonNull
    @Override
    public String toString() {
        return (this == SPEAKER ? "SPEAKER" : "HEADSET")+"{" +
                "streamType=" + streamType +
                ", routeType=" + modeType +
                '}';
    }
}
