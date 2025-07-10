package net.wurstclient.util;

import net.wurstclient.WurstClient;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

public class DiscordRPC {

    private RandomAccessFile pipe;
    private final String clientId;
    private Timer heartbeatTimer;
    private final long timestamp;

    public DiscordRPC(String clientId) {
        this.clientId = clientId;
        timestamp = System.currentTimeMillis() / 1000;
    }

    public void run() {
        if (WurstClient.INSTANCE.getOtfs().discordRpcOtf.isEnabled())
            new Thread(this::connectLoop, "Discord-RPC-Thread").start();
    }

    private void connectLoop() {
        while (true) {
            try {
                pipe = waitForDiscord();

                sendHandshake();
                sendActivity();
                startHeartbeat();

                while (true) {
                    Thread.sleep(5000);
                    if (!isPipeAlive() || !WurstClient.INSTANCE.getOtfs().discordRpcOtf.isEnabled()) throw new Exception("Pipe is closed");
                }

            } catch (Exception e) {
                System.err.println("RPC disconnected: " + e.getMessage());
                stopHeartbeat();
                try { if (pipe != null) pipe.close(); } catch (IOException ignored) {}
                pipe = null;

                try { Thread.sleep(5000); } catch (InterruptedException ignored) {}
                if (WurstClient.INSTANCE.getOtfs().discordRpcOtf.isEnabled())
                    System.out.println("Reconnecting...");
            }
        }
    }

    private void sendHandshake() throws IOException {
        String handshake = "{\"v\":1,\"client_id\":\"" + clientId + "\"}";
        writePacket(0, handshake);
        readPacket();
    }

    private void sendActivity() throws IOException {
        long pid = ProcessHandle.current().pid();

        String activityJson = "{"
                + "\"cmd\":\"SET_ACTIVITY\","
                + "\"args\":{"
                +     "\"pid\":" + pid + ","
                +     "\"activity\":{"
                +         "\"details\":\"by breelock\","
                +         "\"start_timestamp\":" + timestamp + ","
                +         "\"large_image_key\":\"logo\""
                +     "}"
                + "},"
                + "\"nonce\":\"" + UUID.randomUUID() + "\""
                + "}";

        writePacket(1, activityJson);
        readPacket();
    }

    private void startHeartbeat() {
        stopHeartbeat();
        heartbeatTimer = new Timer(true);
        heartbeatTimer.scheduleAtFixedRate(new TimerTask() {
            public void run() {
                try {
                    writePacket(3, "{}");
                } catch (IOException e) {
                    System.err.println("Heartbeat error: " + e.getMessage());
                    stopHeartbeat();
                }
            }
        }, 0, 15000);
    }

    private void stopHeartbeat() {
        if (heartbeatTimer != null) {
            heartbeatTimer.cancel();
            heartbeatTimer = null;
        }
    }

    private void writePacket(int opcode, String json) throws IOException {
        byte[] payload = json.getBytes(StandardCharsets.UTF_8);
        ByteBuffer buffer = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN);
        buffer.putInt(opcode);
        buffer.putInt(payload.length);
        pipe.write(buffer.array());
        pipe.write(payload);
    }

    private void readPacket() throws IOException {
        byte[] header = new byte[8];
        pipe.readFully(header);
        ByteBuffer buffer = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN);
        int opcode = buffer.getInt();
        int length = buffer.getInt();

        byte[] data = new byte[length];
        pipe.readFully(data);
        String response = new String(data, StandardCharsets.UTF_8);
        System.out.println("Response from discord [OP " + opcode + "]: " + response);
    }

    private boolean isPipeAlive() {
        try {
            writePacket(3, "{}");
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private RandomAccessFile waitForDiscord() {
        while (true) {
            if (WurstClient.INSTANCE.getOtfs().discordRpcOtf.isEnabled()) {
                RandomAccessFile pipe = findDiscordPipe();
                if (pipe != null)
                    return pipe;
            }
            try { Thread.sleep(5000); } catch (InterruptedException ignored) {}
        }
    }

    private RandomAccessFile findDiscordPipe() {
        for (int i = 0; i < 10; i++) {
            String path = "\\\\.\\pipe\\discord-ipc-" + i;
            try {
                return new RandomAccessFile(path, "rw");
            } catch (IOException ignored) {}
        }
        return null;
    }
}
