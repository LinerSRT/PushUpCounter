package st.pushupcounter.websocket;

import androidx.annotation.NonNull;

import java.util.Arrays;

public class Payload {
    private final int opcode;
    private final byte[] data;
    private final boolean isCloseEcho;

    public Payload(int opcode, byte[] data, boolean isCloseEcho) {
        this.opcode = opcode;
        this.data = data;
        this.isCloseEcho = isCloseEcho;
    }
    public int getOpcode() {
        return opcode;
    }

    public byte[] getData() {
        return data;
    }

    public boolean isCloseEcho() {
        return isCloseEcho;
    }

    @NonNull
    @Override
    public String toString() {
        return "Payload{" +
                "opcode=" + opcode +
                ", data=" + Arrays.toString(data) +
                ", isCloseEcho=" + isCloseEcho +
                '}';
    }
}
