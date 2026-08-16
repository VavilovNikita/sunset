package com.sunsetbeach.service;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Raw ESC/POS-over-TCP delivery (port 9100 by convention) - the printer is on the same local
 * network as the server, no bridge/VPN/cloud relay involved. A short, mandatory connect timeout
 * is the main defense against a dead/unplugged printer hanging the caller; see the class
 * comment on the residual risk this doesn't cover.
 */
@Component
public class PrinterClient {

    private final int connectTimeoutMillis;

    public PrinterClient(@Value("${app.printing.connect-timeout-ms:2000}") int connectTimeoutMillis) {
        this.connectTimeoutMillis = connectTimeoutMillis;
    }

    /**
     * Opens a socket, writes the payload, closes it. Bounded by {@code connectTimeoutMillis}
     * for both connection setup ({@link Socket#connect}) and any blocking read ({@link
     * Socket#setSoTimeout} - unused here since nothing is read back, kept defensive). Writing
     * the payload itself has no explicit timeout: for the small payloads this app sends (a
     * receipt is at most a few KB), it fits well within the OS's TCP send buffer and returns
     * without blocking even against a printer that accepted the connection but stopped reading
     * ("hung") - a genuinely wedged peer that also fills the send buffer would still block past
     * this timeout, which is a known, accepted residual risk rather than something this method
     * silently claims to solve.
     */
    public void send(String host, int port, byte[] payload) throws IOException {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), connectTimeoutMillis);
            socket.setSoTimeout(connectTimeoutMillis);
            OutputStream out = socket.getOutputStream();
            out.write(payload);
            out.flush();
        }
    }
}
