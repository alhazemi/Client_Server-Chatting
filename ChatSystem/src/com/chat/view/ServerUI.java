package com.chat.view;

import com.chat.controller.ServerController;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;

public class ServerUI extends JFrame {

    // تغيير إلى JEditorPane لدعم HTML والروابط
    private JEditorPane chatArea;
    private JTextArea messageArea;
    private JButton sendButton;
    private JButton emojiButton;
    private JButton fileButton;
    private JDialog emojiDialog;
    private ServerController controller;

    private final String[] emojis = {
        "😀", "😁", "😂", "🤣", "😃", "😄", "😅", "😆", "😉", "😊", "😋", "😎", "😍", "😘", "🥰", "😗", "😙", "😚", 
        "🙂", "🤗", "🤩", "🤔", "🤨", "😐", "😑", "😶", "🙄", "😏", "😣", "😥", "😮", "🤐", "😯", "😪", "😫", "😴", 
        "😌", "😛", "😜", "😝", "🤤", "😒", "😓", "😔", "😕", "🙃", "🤑", "😲", "☹", "🙁", "😖", "😞", "😟", "😤", 
        "😢", "😭", "😦", "😧", "😨", "😩", "🤯", "😬", "😰", "😱", "🥵", "🥶", "😳", "🤪", "😵", "😡"
    };

    public ServerUI() {
        setTitle("Server Chat");
        setSize(450, 550);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));

        JLabel label = new JLabel("SERVER CHAT", SwingConstants.CENTER);
        label.setFont(new Font("Arial", Font.BOLD, 22));
        label.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
        add(label, BorderLayout.NORTH);

        // تغيير إلى JEditorPane
        chatArea = new JEditorPane();
        chatArea.setContentType("text/html");
        chatArea.setEditable(false);
        chatArea.setText("<html><body style='font-family: Arial; padding: 10px;'>"
                + "<div>Server started. Waiting for connections...</div></body></html>");
        
        // إضافة مستمع للروابط لفتح الملفات
        chatArea.addHyperlinkListener(new HyperlinkListener() {
            @Override
            public void hyperlinkUpdate(HyperlinkEvent e) {
                if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                    try {
                        Desktop.getDesktop().open(new File(new URI(e.getURL().toString()).getPath()));
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(
                            ServerUI.this,
                            "Cannot open file: " + ex.getMessage(),
                            "Error",
                            JOptionPane.ERROR_MESSAGE
                        );
                    }
                }
            }
        });
        
        add(new JScrollPane(chatArea), BorderLayout.CENTER);

        JPanel inputPanel = new JPanel(new BorderLayout(5, 5));
        messageArea = new JTextArea(3, 30);
        messageArea.setLineWrap(true);
        messageArea.setWrapStyleWord(true);
        inputPanel.add(new JScrollPane(messageArea), BorderLayout.CENTER);

        // زر الإيموجي
        emojiButton = new JButton("😊");
        emojiButton.setBackground(Color.yellow);
        emojiButton.setFocusPainted(false);
        emojiButton.addActionListener(e -> showEmojiPanel());

        // زر اختيار الملف
        fileButton = new JButton("📎");
        fileButton.setBackground(Color.lightGray);
        fileButton.setFocusPainted(false);
        fileButton.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                File file = chooser.getSelectedFile();
                controller.sendFile(file);
            }
        });

        // زر الإرسال
        sendButton = new JButton("Send");
        sendButton.setBackground(new Color(0, 122, 255));
        sendButton.setForeground(Color.WHITE);
        sendButton.setFocusPainted(false);
        sendButton.addActionListener(e -> {
            String msg = messageArea.getText().trim();
            if (!msg.isEmpty()) {
                controller.sendMessage(msg);
                messageArea.setText("");
            }
        });

        JPanel buttonPanel = new JPanel(new BorderLayout(5, 5));
        buttonPanel.add(emojiButton, BorderLayout.WEST);
        buttonPanel.add(fileButton, BorderLayout.CENTER);
        buttonPanel.add(sendButton, BorderLayout.EAST);

        inputPanel.add(buttonPanel, BorderLayout.EAST);
        add(inputPanel, BorderLayout.SOUTH);

        controller = new ServerController(this);
        addWindowListener(new WindowAdapter() {
            public void windowOpened(WindowEvent evt) {
                controller.startServer();
            }
        });

        setVisible(true);
    }

    private void showEmojiPanel() {
        if (emojiDialog == null) {
            emojiDialog = new JDialog(this, false);
            emojiDialog.setUndecorated(true);
            emojiDialog.setSize(300, 300);
            emojiDialog.setLocationRelativeTo(emojiButton);
            JPanel panel = new JPanel(new GridLayout(5, 8, 4, 4));
            panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
            for (String emoji : emojis) {
                JButton btn = new JButton(emoji);
                btn.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 18));
                btn.setMargin(new Insets(2, 2, 2, 2));
                btn.addActionListener(e -> {
                    messageArea.append(emoji);
                    emojiDialog.setVisible(false);
                });
                panel.add(btn);
            }
            emojiDialog.add(new JScrollPane(panel));
        }
        emojiDialog.setVisible(true);
    }
    public void appendMessage(String sender, String message) {
        SwingUtilities.invokeLater(() -> {
            try {
                String escapedMessage = message
                    .replace("&", "&amp;")
                    .replace("<", "&lt;")
                    .replace(">", "&gt;")
                    .replace("\"", "&quot;");
                
                String htmlMsg = String.format(
                    "<div style='margin-bottom: 10px;'>" +
                    "<span style='color: #2E86C1; font-weight: bold;'>%s:</span> " +
                    "<span>%s</span>" +
                    "</div>",
                    sender, 
                    escapedMessage
                );
                
                String currentContent = chatArea.getText();
                int bodyEndIndex = currentContent.indexOf("</body>");
                if (bodyEndIndex == -1) {
                    currentContent += htmlMsg;
                } else {
                    currentContent = currentContent.substring(0, bodyEndIndex) 
                                   + htmlMsg 
                                   + currentContent.substring(bodyEndIndex);
                }
                
                chatArea.setText(currentContent);
                chatArea.setCaretPosition(chatArea.getDocument().getLength());
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
    public void appendFileMessage(String sender, String fileName, String filePath) {
        SwingUtilities.invokeLater(() -> {
            try {
                String link = String.format(
                    "<a href='file://%s' style='color: #27AE60; text-decoration: none;'>%s</a>", 
                    filePath.replace("\\", "/"), 
                    fileName
                );
                
                String htmlMsg = String.format(
                    "<div style='margin-bottom: 10px;'>" +
                    "<span style='color: #2E86C1; font-weight: bold;'>%s:</span> " +
                    "<span>📁 %s</span>" +
                    "</div>",
                    sender, 
                    link
                );
                
                String currentContent = chatArea.getText();
                
                // البحث عن موقع </body> لإدراج المحتوى الجديد قبله
                int bodyEndIndex = currentContent.indexOf("</body>");
                if (bodyEndIndex == -1) {
                    currentContent += htmlMsg;
                } else {
                    currentContent = currentContent.substring(0, bodyEndIndex) 
                                   + htmlMsg 
                                   + currentContent.substring(bodyEndIndex);
                }
                
                chatArea.setText(currentContent);
                
                // التمرير إلى الأسفل
                chatArea.setCaretPosition(chatArea.getDocument().getLength());
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}