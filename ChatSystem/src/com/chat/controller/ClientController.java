package com.chat.controller;

import com.chat.view.ClientUI;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Random;

public class ClientController {

    private ClientUI ui;
    private Socket chatSocket;
    private PrintWriter chatWriter;
    private BufferedReader chatReader;
    private int filePort; 

    private static final String HOST = "localhost";
    private static final int CHAT_PORT = 4789;

    public ClientController(ClientUI ui) {
        this.ui = ui;
        this.filePort = 5000 + new Random().nextInt(1000); 
    }

    public void startClient() {
        new Thread(() -> {
            try {
                chatSocket = new Socket(HOST, CHAT_PORT);
                chatReader = new BufferedReader(new InputStreamReader(chatSocket.getInputStream()));
                chatWriter = new PrintWriter(chatSocket.getOutputStream(), true);
                chatWriter.println("FILE_PORT:" + filePort);
                
                ui.appendMessage("System", "Connected to chat server.");

                String line;
                while ((line = chatReader.readLine()) != null) {
                    if (line.startsWith("FILE:")) {
                        String fileName = line.substring(5).trim();
                        receiveFile(fileName);
                    } else {
                        ui.appendMessage("Server", line);
                    }
                }
            } catch (IOException e) {
                ui.appendMessage("System", "Chat error: " + e.getMessage());
            }
        }).start();
        new Thread(() -> {
            try (ServerSocket fileServer = new ServerSocket(filePort)) {
                ui.appendMessage("System", "File server listening on port " + filePort);
                while (true) {
                    try (Socket fsock = fileServer.accept();
                         DataInputStream dis = new DataInputStream(fsock.getInputStream())) {

                        String fileName = dis.readUTF();
                        long fileSize = dis.readLong();
                        File downloadsDir = new File("downloads");
                        if (!downloadsDir.exists()) downloadsDir.mkdir();
                        
                        File received = new File(downloadsDir, fileName);
                        try (FileOutputStream fos = new FileOutputStream(received)) {
                            byte[] buffer = new byte[4096];
                            long remaining = fileSize;
                            int read;
                            while (remaining > 0 && 
                                  (read = dis.read(buffer, 0, (int)Math.min(buffer.length, remaining))) > 0) {
                                fos.write(buffer, 0, read);
                                remaining -= read;
                            }
                        }
                        ui.appendFileMessage("Server", fileName, received.getAbsolutePath());
                    } catch (IOException ex) {
                        ui.appendMessage("System", "File receive error: " + ex.getMessage());
                    }
                }
            } catch (IOException e) {
                ui.appendMessage("System", "File server error: " + e.getMessage());
            }
        }).start();
    }
    public void sendMessage(String msg) {
        if (chatWriter != null) {
            chatWriter.println(msg);
            ui.appendMessage("You", msg);
        } else {
            ui.appendMessage("System", "Error: Not connected to server.");
        }
    }
    public void sendFile(File file) {
        new Thread(() -> {
            try (Socket fsock = new Socket(HOST, filePort);
                 DataOutputStream dos = new DataOutputStream(fsock.getOutputStream());
                 FileInputStream fis = new FileInputStream(file)) {
                chatWriter.println("FILE:" + file.getName());
                dos.writeUTF(file.getName());
                dos.writeLong(file.length());
                
                byte[] buffer = new byte[4096];
                int read;
                while ((read = fis.read(buffer)) > 0) {
                    dos.write(buffer, 0, read);
                }
                ui.appendFileMessage("You", file.getName(), file.getAbsolutePath());
            } catch (IOException ex) {
                ui.appendMessage("System", "File send error: " + ex.getMessage());
            }
        }).start();
    }
    private void receiveFile(String fileName) {
    }
}