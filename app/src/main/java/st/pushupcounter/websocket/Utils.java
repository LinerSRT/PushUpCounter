package st.pushupcounter.websocket;


import android.util.Base64;

public class Utils {

    public static byte[] to2ByteArray(int value) {
        return new byte[]{(byte) (value >>> 8), (byte) value};
    }

    public static byte[] to8ByteArray(int value) {
        return new byte[]{0, 0, 0, 0, (byte) (value >>> 24), (byte) (value >>> 16), (byte) (value >>> 8), (byte) value};
    }

    public static int fromByteArray(byte[] bytes) {
        return bytes[0] << 24 | (bytes[1] & 0xFF) << 16 | (bytes[2] & 0xFF) << 8 | (bytes[3] & 0xFF);
    }

    public static String encodeToBase64String(byte[] data) {
        return Base64.encodeToString(data, android.util.Base64.NO_WRAP);
    }
}
