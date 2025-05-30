package com.chat.controller;

import com.chat.view.ServerUI;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.util.*;

public class ServerController {

    private ServerUI ui;
    private ServerSocket serverSocket;
    private Map<Socket, Integer> clientFilePorts = new HashMap<>();

    public ServerController(ServerUI ui) {
        this.ui = ui;
    }

    public void startServer() {
        try {
            serverSocket = new ServerSocket(4789);
            ui.appendMessage("System", "Server started on port 4789");
            
            new Thread(() -> {
                while (true) {
                    try {
                        Socket clientSocket = serverSocket.accept();
                        ui.appendMessage("System", "Client connected: " + clientSocket.getRemoteSocketAddress());
                        
                        new Thread(new ClientHandler(clientSocket)).start();
                        
                    } catch (IOException e) {
                        ui.appendMessage("System", "Error accepting client: " + e.getMessage());
                    }
                }
            }).start();
            
        } catch (IOException e) {
            ui.appendMessage("System", "Error starting server: " + e.getMessage());
        }
    }

    public void sendMessage(String message) {
        for (Socket client : clientFilePorts.keySet()) {
            try {
                PrintWriter out = new PrintWriter(client.getOutputStream(), true);
                out.println(message);
                ui.appendMessage("You", message);
            } catch (IOException e) {
                ui.appendMessage("System", "Error sending message: " + e.getMessage());
            }
        }
    }
    
    public void sendFile(File file) {
        for (Socket client : clientFilePorts.keySet()) {
            try {
                PrintWriter out = new PrintWriter(client.getOutputStream(), true);
                out.println("FILE:" + file.getName());
                int port = clientFilePorts.get(client);
                try (Socket fsock = new Socket(client.getInetAddress(), port);
                     DataOutputStream dos = new DataOutputStream(fsock.getOutputStream());
                     FileInputStream fis = new FileInputStream(file)) {
                    
                    dos.writeUTF(file.getName());
                    dos.writeLong(file.length());
                    
                    byte[] buffer = new byte[4096];
                    int read;
                    while ((read = fis.read(buffer)) > 0) {
                        dos.write(buffer, 0, read);
                    }
                }
                ui.appendFileMessage("You", file.getName(), file.getAbsolutePath());
            } catch (IOException ex) {
                ui.appendMessage("System", "File send error: " + ex.getMessage());
            }
        }
    }

    private class ClientHandler implements Runnable {
        private Socket clientSocket;
        private BufferedReader in;
        private PrintWriter out;

        public ClientHandler(Socket socket) throws IOException {
            this.clientSocket = socket;
            this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.out = new PrintWriter(socket.getOutputStream(), true);
        }

        @Override
        public void run() {
            try {
                clientFilePorts.put(clientSocket, null);
                
                String line;
                while ((line = in.readLine()) != null) {
                    if (line.startsWith("FILE_PORT:")) {
                        int port = Integer.parseInt(line.substring(10));
                        clientFilePorts.put(clientSocket, port);
                        ui.appendMessage("System", "Client file port: " + port);
                    } else if (line.startsWith("FILE:")) {
                        String fileName = line.substring(5).trim();
                        receiveFile(fileName);
                    } else {
                        ui.appendMessage("Client", line);
                    }
                }
            } catch (IOException e) {
                ui.appendMessage("System", "Client disconnected: " + e.getMessage());
            } finally {
                clientFilePorts.remove(clientSocket);
                try {
                    clientSocket.close();
                } catch (IOException e) {
                    ui.appendMessage("System", "Error closing socket: " + e.getMessage());
                }
            }
        }
        
        private void receiveFile(String fileName) {
            try {
                int port = clientFilePorts.get(clientSocket);
                if (port == 0) {
                    ui.appendMessage("System", "File port not set for client");
                    return;
                }
                try (Socket fsock = new Socket(clientSocket.getInetAddress(), port);
                     DataInputStream dis = new DataInputStream(fsock.getInputStream())) {
                    
                    String receivedFileName = dis.readUTF();
                    long fileSize = dis.readLong();
                    File receivedDir = new File("server_received");
                    if (!receivedDir.exists()) receivedDir.mkdir();
                    File received = new File(receivedDir, receivedFileName);
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
                    ui.appendFileMessage("Client", receivedFileName, received.getAbsolutePath());
                }
            } catch (IOException ex) {
                ui.appendMessage("System", "File receive error: " + ex.getMessage());
            }
        }
    }
}