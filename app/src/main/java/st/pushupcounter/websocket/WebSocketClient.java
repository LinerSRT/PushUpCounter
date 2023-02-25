package st.pushupcounter.websocket;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import javax.net.SocketFactory;
import javax.net.ssl.SSLSocketFactory;

@SuppressWarnings("unused")
public abstract class WebSocketClient {
    public static final int CLOSE_CODE_NORMAL = 1000;
    private static final int MAX_HEADER_SIZE = 16392;
    private static final String GUID = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";
    private static final int OPCODE_CONTINUATION = 0x0;
    private static final int OPCODE_TEXT = 0x1;
    private static final int OPCODE_BINARY = 0x2;
    private static final int OPCODE_CLOSE = 0x8;
    private static final int OPCODE_PING = 0x9;
    private static final int OPCODE_PONG = 0xA;
    private final Object globalLock;
    private final URI uri;
    private final SecureRandom secureRandom;
    private int connectTimeout;
    private int readTimeout;
    private boolean automaticReconnection;
    private long waitTimeBeforeReconnection;
    private volatile boolean isRunning;
    private final Map<String, String> headers;
    private volatile WebSocketConnection webSocketConnection;
    private volatile Thread reconnectionThread;
    private SSLSocketFactory sslSocketFactory;
    private volatile Timer closeTimer;


    public WebSocketClient(URI uri) {
        this.globalLock = new Object();
        this.uri = uri;
        this.secureRandom = new SecureRandom();
        this.connectTimeout = 0;
        this.readTimeout = 0;
        this.automaticReconnection = false;
        this.waitTimeBeforeReconnection = 0;
        this.isRunning = false;
        this.headers = new HashMap<>();
        webSocketConnection = new WebSocketConnection();
    }

    public void onOpen(WebSocketClient webSocketClient) {

    }


    public void onTextReceived(WebSocketClient webSocketClient, String message) {

    }


    public void onBinaryReceived(WebSocketClient webSocketClient, byte[] data) {

    }

    public void onPingReceived(WebSocketClient webSocketClient, byte[] data) {

    }

    public void onPongReceived(WebSocketClient webSocketClient, byte[] data) {

    }

    public void onException(Exception e) {

    }

    public void onCloseReceived(WebSocketClient webSocketClient, int reason, String description) {

    }


    public void addHeader(String key, String value) {
        synchronized (globalLock) {
            if (isRunning) {
                throw new IllegalStateException("Cannot add header while WebSocketClient is running");
            }
            this.headers.put(key, value);
        }
    }

    public void setConnectTimeout(int connectTimeout) {
        synchronized (globalLock) {
            if (isRunning) {
                throw new IllegalStateException("Cannot set connect timeout while WebSocketClient is running");
            } else if (connectTimeout < 0) {
                throw new IllegalStateException("Connect timeout must be greater or equal than zero");
            }
            this.connectTimeout = connectTimeout;
        }
    }


    public void setReadTimeout(int readTimeout) {
        synchronized (globalLock) {
            if (isRunning) {
                throw new IllegalStateException("Cannot set read timeout while WebSocketClient is running");
            } else if (readTimeout < 0) {
                throw new IllegalStateException("Read timeout must be greater or equal than zero");
            }
            this.readTimeout = readTimeout;
        }
    }


    public void enableAutomaticReconnection(long waitTimeBeforeReconnection) {
        synchronized (globalLock) {
            if (isRunning) {
                throw new IllegalStateException(
                        "Cannot enable automatic reconnection while WebSocketClient is running");
            } else if (waitTimeBeforeReconnection < 0) {
                throw new IllegalStateException("Wait time between reconnections must be greater or equal than zero");
            }
            this.automaticReconnection = true;
            this.waitTimeBeforeReconnection = waitTimeBeforeReconnection;
        }
    }

    public void disableAutomaticReconnection() {
        synchronized (globalLock) {
            if (isRunning) {
                throw new IllegalStateException(
                        "Cannot disable automatic reconnection while WebSocketClient is running");
            }
            this.automaticReconnection = false;
        }
    }

    public void connect() {
        synchronized (globalLock) {
            if (isRunning) {
                throw new IllegalStateException("WebSocketClient is not reusable");
            }

            this.isRunning = true;
            createAndStartConnectionThread();
        }
    }

    public boolean isRunning() {
        return isRunning;
    }

    public void setSSLSocketFactory(SSLSocketFactory sslSocketFactory) {
        synchronized (globalLock) {
            if (isRunning) {
                throw new IllegalStateException("Cannot set SSLSocketFactory while WebSocketClient is running");
            } else if (sslSocketFactory == null) {
                throw new IllegalStateException("SSLSocketFactory cannot be null");
            }
            this.sslSocketFactory = sslSocketFactory;
        }
    }

    private void createAndStartConnectionThread() {
        new Thread(() -> {
            try {
                boolean success = webSocketConnection.createAndConnectTCPSocket();
                if (success) {
                    webSocketConnection.startConnection();
                }
            } catch (Exception e) {
                synchronized (globalLock) {
                    if (isRunning) {
                        webSocketConnection.closeInternal();

                        onException(e);

                        if (e instanceof IOException && automaticReconnection) {
                            createAndStartReconnectionThread();
                        }
                    }
                }
            }
        }).start();
    }

    private void createAndStartReconnectionThread() {
        reconnectionThread = new Thread(() -> {
            try {
                Thread.sleep(waitTimeBeforeReconnection);

                synchronized (globalLock) {
                    if (isRunning) {
                        webSocketConnection = new WebSocketConnection();
                        createAndStartConnectionThread();
                    }
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
        reconnectionThread.start();
    }


    private void notifyOnOpen() {
        synchronized (globalLock) {
            if (isRunning) {
                onOpen(this);
            }
        }
    }


    private void notifyOnTextReceived(String message) {
        synchronized (globalLock) {
            if (isRunning) {
                onTextReceived(this, message);
            }
        }
    }


    private void notifyOnBinaryReceived(byte[] data) {
        synchronized (globalLock) {
            if (isRunning) {
                onBinaryReceived(this, data);
            }
        }
    }


    private void notifyOnPingReceived(byte[] data) {
        synchronized (globalLock) {
            if (isRunning) {
                onPingReceived(this, data);
            }
        }
    }


    private void notifyOnPongReceived(byte[] data) {
        synchronized (globalLock) {
            if (isRunning) {
                onPongReceived(this, data);
            }
        }
    }


    private void notifyOnException(Exception e) {
        synchronized (globalLock) {
            if (isRunning) {
                onException(e);
            }
        }
    }


    private void notifyOnCloseReceived(int reason, String description) {
        synchronized (globalLock) {
            if (isRunning) {
                onCloseReceived(this, reason, description);
            }
        }
    }

    private void forceClose() {
        new Thread(() -> {
            synchronized (globalLock) {
                isRunning = false;
                if (reconnectionThread != null) {
                    reconnectionThread.interrupt();
                }
                webSocketConnection.closeInternal();
            }
        }).start();
    }


    public void send(String message) {
        byte[] data = message.getBytes(StandardCharsets.UTF_8);
        final Payload payload = new Payload(OPCODE_TEXT, data, false);
        new Thread(() -> webSocketConnection.sendInternal(payload)).start();
    }


    public void send(byte[] data) {
        final Payload payload = new Payload(OPCODE_BINARY, data, false);
        new Thread(() -> webSocketConnection.sendInternal(payload)).start();
    }

    public void sendPing(byte[] data) {
        if (data != null && data.length > 125) {
            throw new IllegalArgumentException("Control frame payload cannot be greater than 125 bytes");
        }
        final Payload payload = new Payload(OPCODE_PING, data, false);
        new Thread(() -> webSocketConnection.sendInternal(payload)).start();
    }


    public void sendPong(byte[] data) {
        if (data != null && data.length > 125) {
            throw new IllegalArgumentException("Control frame payload cannot be greater than 125 bytes");
        }
        final Payload payload = new Payload(OPCODE_PONG, data, false);
        new Thread(() -> webSocketConnection.sendInternal(payload)).start();
    }

    public void close(final int timeout, int code, String reason) {
        if (timeout == 0) {
            forceClose();
        } else if (code < 0 || code >= 5000) {
            throw new IllegalArgumentException("Close frame code must be greater or equal than zero and less than 5000");
        } else {
            byte[] internalReason = new byte[0];
            if (reason != null) {
                internalReason = reason.getBytes(StandardCharsets.UTF_8);
                if (internalReason.length > 123) {
                    throw new IllegalArgumentException("Close frame reason is too large");
                }
            }
            byte[] codeLength = Utils.to2ByteArray(code);
            byte[] data = Arrays.copyOf(codeLength, 2 + internalReason.length);
            System.arraycopy(internalReason, 0, data, codeLength.length, internalReason.length);
            final Payload payload = new Payload(OPCODE_CLOSE, data, false);
            new Thread(() -> webSocketConnection.sendInternal(payload)).start();
            closeTimer = new Timer();
            closeTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    forceClose();
                }
            }, timeout);
        }
    }

    @SuppressWarnings("UnnecessaryLabelOnBreakStatement")
    private class WebSocketConnection {
        private volatile boolean pendingMessages;
        private volatile boolean isClosed;
        private volatile boolean isClosing;
        private final Queue<Payload> queue;
        private final Object internalLock;
        private final Thread writerThread;
        private Socket socket;
        private BufferedInputStream bufferedInputStream;
        private BufferedOutputStream bufferedOutputStream;

        private WebSocketConnection() {
            this.pendingMessages = false;
            this.isClosed = false;
            this.isClosing = false;
            this.queue = new LinkedList<>();
            this.internalLock = new Object();

            this.writerThread = new Thread(() -> {
                synchronized (internalLock) {
                    while (true) {
                        if (!pendingMessages) {
                            try {
                                internalLock.wait();
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                        pendingMessages = false;
                        if (socket.isClosed()) {
                            return;
                        } else {
                            while (queue.size() > 0) {
                                Payload payload = queue.poll();
                                if (payload != null) {
                                    int opcode = payload.getOpcode();
                                    byte[] data = payload.getData();
                                    try {
                                        send(opcode, data);
                                        if (payload.isCloseEcho()) {
                                            closeInternalInsecure();
                                        }
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                        return;
                                    }
                                } else {
                                    return;
                                }
                            }
                        }
                    }
                }
            });
        }


        private boolean createAndConnectTCPSocket() throws IOException {
            synchronized (internalLock) {
                if (!isClosed) {
                    String scheme = uri.getScheme();
                    int port = uri.getPort();
                    if (scheme != null) {
                        if (scheme.equals("ws")) {
                            SocketFactory socketFactory = SocketFactory.getDefault();
                            socket = socketFactory.createSocket();
                            socket.setSoTimeout(readTimeout);

                            if (port != -1) {
                                socket.connect(new InetSocketAddress(uri.getHost(), port), connectTimeout);
                            } else {
                                socket.connect(new InetSocketAddress(uri.getHost(), 80), connectTimeout);
                            }
                        } else if (scheme.equals("wss")) {
                            if (sslSocketFactory == null) {
                                sslSocketFactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
                            }
                            socket = sslSocketFactory.createSocket();
                            socket.setSoTimeout(readTimeout);

                            if (port != -1) {
                                socket.connect(new InetSocketAddress(uri.getHost(), port), connectTimeout);
                            } else {
                                socket.connect(new InetSocketAddress(uri.getHost(), 443), connectTimeout);
                            }
                        } else {
                            throw new RuntimeException("The scheme component of the URI should be ws or wss");
                        }
                    } else {
                        throw new RuntimeException("The scheme component of the URI cannot be null");
                    }

                    return true;
                }

                return false;
            }
        }

        private void startConnection() throws IOException {
            bufferedOutputStream = new BufferedOutputStream(socket.getOutputStream(), 65536);

            byte[] key = new byte[16];
            Random random = new Random();
            random.nextBytes(key);
            String base64Key = Utils.encodeToBase64String(key);

            byte[] handshake = createHandshake(base64Key);
            bufferedOutputStream.write(handshake);
            bufferedOutputStream.flush();

            InputStream inputStream = socket.getInputStream();
            verifyServerHandshake(inputStream, base64Key);
            notifyOnOpen();
            writerThread.start();

            bufferedInputStream = new BufferedInputStream(inputStream, 65536);
            read();
        }


        private byte[] createHandshake(String base64Key) {
            StringBuilder builder = new StringBuilder();
            String path = uri.getRawPath();
            String query = uri.getRawQuery();
            String requestUri;
            if (path != null && !path.isEmpty()) {
                requestUri = path;
            } else {
                requestUri = "/";
            }
            if (query != null && !query.isEmpty()) {
                requestUri = requestUri + "?" + query;
            }
            builder.append("GET ").append(requestUri).append(" HTTP/1.1");
            builder.append("\r\n");

            String host;
            if (uri.getPort() == -1) {
                host = uri.getHost();
            } else {
                host = uri.getHost() + ":" + uri.getPort();
            }
            builder.append("Host: ").append(host);
            builder.append("\r\n");
            builder.append("Upgrade: websocket");
            builder.append("\r\n");
            builder.append("Connection: Upgrade");
            builder.append("\r\n");
            builder.append("Sec-WebSocket-Key: ").append(base64Key);
            builder.append("\r\n");
            builder.append("Sec-WebSocket-Version: 13");
            builder.append("\r\n");

            for (Map.Entry<String, String> entry : headers.entrySet()) {
                builder.append(entry.getKey()).append(": ").append(entry.getValue());
                builder.append("\r\n");
            }
            builder.append("\r\n");
            String handshake = builder.toString();
            return handshake.getBytes(StandardCharsets.US_ASCII);
        }


        private void verifyServerHandshake(InputStream inputStream, String secWebSocketKey) throws IOException {
            Queue<String> lines = new LinkedList<>();
            StringBuilder builder = new StringBuilder();
            boolean lastLineBreak = false;
            int bytesRead = 0;

            outer:
            do {
                inner:
                do {
                    int result = inputStream.read();
                    if (result == -1) {
                        throw new IOException("Unexpected end of stream");
                    }

                    char c = (char) result;
                    bytesRead++;
                    if (c == '\r') {
                        result = inputStream.read();
                        if (result == -1) {
                            throw new IOException("Unexpected end of stream");
                        }

                        c = (char) result;
                        bytesRead++;
                        if (c == '\n') {
                            if (lastLineBreak) {
                                break outer;
                            }
                            lastLineBreak = true;
                            break inner;
                        } else {
                            throw new RuntimeException("Invalid handshake format");
                        }
                    } else if (c == '\n') {
                        if (lastLineBreak) {
                            break outer;
                        }
                        lastLineBreak = true;
                        break inner;
                    } else {
                        lastLineBreak = false;
                        builder.append(c);
                    }
                } while (bytesRead <= MAX_HEADER_SIZE);

                lines.offer(builder.toString());
                builder.setLength(0);
            } while (bytesRead <= MAX_HEADER_SIZE);

            if (bytesRead > MAX_HEADER_SIZE) {
                throw new RuntimeException("Entity too large");
            }

            String statusLine = lines.poll();
            if (statusLine == null) {
                throw new RuntimeException("There is no status line");
            }

            String[] statusLineParts = statusLine.split(" ");
            if (statusLineParts.length > 1) {
                String statusCode = statusLineParts[1];
                if (!statusCode.equals("101")) {
                    throw new RuntimeException("Invalid status code. Expected 101, received: " + statusCode);
                }
            } else {
                throw new RuntimeException("Invalid status line format");
            }

            Map<String, String> headers = new HashMap<>();
            for (String s : lines) {
                String[] parts = s.split(":", 2);
                if (parts.length == 2) {
                    headers.put(parts[0].trim(), parts[1].trim());
                } else {
                    throw new RuntimeException("Invalid headers format");
                }
            }

            String upgradeValue = headers.get("Upgrade");
            if (upgradeValue == null) {
                throw new RuntimeException("There is no header named Upgrade");
            }
            upgradeValue = upgradeValue.toLowerCase();
            if (!upgradeValue.equals("websocket")) {
                throw new RuntimeException("Invalid value for header Upgrade. Expected: websocket, received: " + upgradeValue);
            }

            String connectionValue = headers.get("Connection");
            if (connectionValue == null) {
                throw new RuntimeException("There is no header named Connection");
            }
            connectionValue = connectionValue.toLowerCase();
            if (!connectionValue.equals("upgrade")) {
                throw new RuntimeException("Invalid value for header Connection. Expected: upgrade, received: " + connectionValue);
            }

            String secWebSocketAcceptValue = headers.get("Sec-WebSocket-Accept");
            if (secWebSocketAcceptValue == null) {
                throw new RuntimeException("There is no header named Sec-WebSocket-Accept");
            }

            String keyConcatenation = secWebSocketKey + GUID;
            try {
                MessageDigest md = MessageDigest.getInstance("SHA-1");
                md.update(keyConcatenation.getBytes(StandardCharsets.US_ASCII));
                byte[] sha1 = md.digest();
                String secWebSocketAccept = Utils.encodeToBase64String(sha1);
                if (!secWebSocketAcceptValue.equals(secWebSocketAccept)) {
                    throw new RuntimeException("Invalid value for header Sec-WebSocket-Accept. Expected: " + secWebSocketAccept + ", received: " + secWebSocketAcceptValue);
                }
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException("Your platform does not support the SHA-1 algorithm");
            }
        }


        private void send(int opcode, byte[] payload) throws IOException {


            int nextPosition;


            byte[] frame;


            int length = payload == null ? 0 : payload.length;

            if (length < 126) {


                frame = new byte[6 + length];


                frame[0] = (byte) (-128 | opcode);
                frame[1] = (byte) (-128 | length);


                nextPosition = 2;
            } else if (length < 65536) {


                frame = new byte[8 + length];


                frame[0] = (byte) (-128 | opcode);
                frame[1] = -2;


                byte[] array = Utils.to2ByteArray(length);
                frame[2] = array[0];
                frame[3] = array[1];


                nextPosition = 4;
            } else {


                frame = new byte[14 + length];


                frame[0] = (byte) (-128 | opcode);
                frame[1] = -1;


                byte[] array = Utils.to8ByteArray(length);
                frame[2] = array[0];
                frame[3] = array[1];
                frame[4] = array[2];
                frame[5] = array[3];
                frame[6] = array[4];
                frame[7] = array[5];
                frame[8] = array[6];
                frame[9] = array[7];


                nextPosition = 10;
            }


            byte[] mask = new byte[4];
            secureRandom.nextBytes(mask);


            frame[nextPosition] = mask[0];
            frame[nextPosition + 1] = mask[1];
            frame[nextPosition + 2] = mask[2];
            frame[nextPosition + 3] = mask[3];
            nextPosition += 4;


            for (int i = 0; i < length; i++) {
                frame[nextPosition] = ((byte) (payload[i] ^ mask[i % 4]));
                nextPosition++;
            }


            bufferedOutputStream.write(frame);
            bufferedOutputStream.flush();
        }

        private void read() throws IOException {

            int opcodeFragment = -1;
            LinkedList<byte[]> messageParts = new LinkedList<>();


            int firstByte;


            while ((firstByte = bufferedInputStream.read()) != -1) {


                int fin = (firstByte << 24) >>> 31;


                int opcode = (firstByte << 28) >>> 28;


                if (fin == 0x0 && opcodeFragment == -1) {
                    opcodeFragment = opcode;
                }


                int secondByte = bufferedInputStream.read();


                int payloadLength = (secondByte << 25) >>> 25;


                if (payloadLength == 126) {

                    byte[] nextTwoBytes = new byte[2];
                    for (int i = 0; i < 2; i++) {
                        byte b = (byte) bufferedInputStream.read();
                        nextTwoBytes[i] = b;
                    }


                    byte[] integer = new byte[]{0, 0, nextTwoBytes[0], nextTwoBytes[1]};
                    payloadLength = Utils.fromByteArray(integer);
                } else if (payloadLength == 127) {

                    byte[] nextEightBytes = new byte[8];
                    for (int i = 0; i < 8; i++) {
                        byte b = (byte) bufferedInputStream.read();
                        nextEightBytes[i] = b;
                    }


                    byte[] integer = new byte[]{nextEightBytes[4], nextEightBytes[5], nextEightBytes[6],
                            nextEightBytes[7]};
                    payloadLength = Utils.fromByteArray(integer);
                }


                byte[] data = new byte[payloadLength];
                for (int i = 0; i < payloadLength; i++) {
                    byte b = (byte) bufferedInputStream.read();
                    data[i] = b;
                }

                if (fin == 0x1 && opcode == OPCODE_CONTINUATION) {


                    messageParts.add(data);

                    int fullSize = 0;
                    int offset = 0;
                    for (byte[] fragment : messageParts)
                        fullSize += fragment.length;

                    byte[] fullMessage = new byte[fullSize];


                    for (byte[] fragment : messageParts) {
                        System.arraycopy(fragment, 0, fullMessage, offset, fragment.length);
                        offset += fragment.length;
                    }

                    data = fullMessage;
                    messageParts.clear();

                    opcode = opcodeFragment;
                    opcodeFragment = -1;
                } else if (fin == 0x0 && (opcode == OPCODE_CONTINUATION || opcode == OPCODE_TEXT || opcode == OPCODE_BINARY)) {

                    messageParts.add(data);
                    continue;
                }


                switch (opcode) {
                    case OPCODE_TEXT:
                        notifyOnTextReceived(new String(data, StandardCharsets.UTF_8));
                        break;
                    case OPCODE_BINARY:
                        notifyOnBinaryReceived(data);
                        break;
                    case OPCODE_CLOSE:
                        if (data.length > 125) {
                            closeInternal();
                            Exception e = new RuntimeException("Close frame payload is too big");
                            notifyOnException(e);
                            return;
                        } else {
                            int code = getCloseCode(data);
                            String reason = getCloseReason(data);
                            notifyOnCloseReceived(code, reason);
                        }

                        synchronized (internalLock) {
                            if (isClosing) {

                                closeInternalInsecure();
                                return;
                            } else {

                                Payload payload = new Payload(OPCODE_CLOSE, data, true);
                                sendInternalInsecure(payload);
                                break;
                            }
                        }
                    case OPCODE_PING:
                        notifyOnPingReceived(data);
                        sendPong(data);
                        break;
                    case OPCODE_PONG:
                        notifyOnPongReceived(data);
                        break;
                    default:
                        closeInternal();
                        Exception e = new RuntimeException("Unknown opcode: 0x" + Integer.toHexString(opcode));
                        notifyOnException(e);
                        return;
                }
            }

            synchronized (internalLock) {

                if (!isClosing) {

                    throw new IOException("Unexpected end of stream");
                }
            }
        }


        private void sendInternal(Payload payload) {
            synchronized (internalLock) {
                sendInternalInsecure(payload);
            }
        }

        private void sendInternalInsecure(Payload payload) {
            if (!isClosing) {
                if (payload.getOpcode() == OPCODE_CLOSE) {
                    isClosing = true;
                }
                queue.offer(payload);
                pendingMessages = true;
                internalLock.notify();
            }
        }


        private void closeInternal() {
            synchronized (internalLock) {
                closeInternalInsecure();
            }
        }

        private void closeInternalInsecure() {
            try {
                if (!isClosed) {
                    isClosed = true;
                    if (socket != null) {
                        socket.close();
                        pendingMessages = true;
                        internalLock.notify();
                    }
                }

                if (closeTimer != null) {
                    closeTimer.cancel();
                    closeTimer = null;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private int getCloseCode(byte[] data) {
            if (data.length > 1) {
                byte[] baseCode = Arrays.copyOfRange(data, 0, 2);
                return Utils.fromByteArray(new byte[]{0, 0, baseCode[0], baseCode[1]});
            }
            return -1;
        }

        private String getCloseReason(byte[] data) {
            if (data.length > 2) {
                byte[] baseReason = Arrays.copyOfRange(data, 2, data.length);
                return new String(baseReason, StandardCharsets.UTF_8);
            }
            return null;
        }
    }
}
