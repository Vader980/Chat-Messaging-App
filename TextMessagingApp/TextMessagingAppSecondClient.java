import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.net.Socket;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.List;


public class TextMessagingAppSecondClient extends JFrame {
    private JTextArea chatArea;
    private JTextField messageField;
    private JButton sendButton;

    private JTabbedPane tabbedPane;
    private JPanel groupPanel;
    private JTextField groupNameField;
    private JButton createGroupButton;
    private DefaultListModel<String> groupListModel;
    private JList<String> groupList;
    private DefaultListModel<String> participantListModel;
    private JList<String> participantList;
    private JButton startChatButton;
    private JButton checkOnlineButton;
    private JList<String> onlineParticipantsList;
    private DefaultListModel<String> onlineParticipantsListModel;

    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;

    private String username;

    public TextMessagingAppSecondClient() {
        try {
            // Create socket
            socket = new Socket("localhost", 1234);
            // Create input and output streams
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Check if the socket is connected to the server
        if (socket.isConnected()) {
            System.out.println("Connected to the server!");
        } else {
            System.out.println("Not connected to the server!");
        }

        // Prompt user for username
        username = JOptionPane.showInputDialog(this, "Enter your username:");

        // Create UI elements
        chatArea = new JTextArea();
        chatArea.setEditable(false);

        messageField = new JTextField(20);
        messageField.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sendMessage();
            }
        });

        sendButton = new JButton("Send");
        sendButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sendMessage();
            }
        });

        // Create group list
        groupListModel = new DefaultListModel<>();
        groupList = new JList<>(groupListModel);
        groupList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // Create participant list
        participantListModel = new DefaultListModel<>();
        participantList = new JList<>(participantListModel);
        participantList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

        // Create tab control
        tabbedPane = new JTabbedPane();
        add(tabbedPane, BorderLayout.CENTER);

        // Add chat panel to tab control
        JPanel chatPanel = new JPanel(new BorderLayout());
        JPanel messagePanel = new JPanel(new BorderLayout());
        messagePanel.add(messageField, BorderLayout.CENTER);
        messagePanel.add(sendButton, BorderLayout.EAST);
        chatPanel.add(new JScrollPane(chatArea), BorderLayout.CENTER);
        chatPanel.add(messagePanel, BorderLayout.SOUTH);
        tabbedPane.addTab("Chat", chatPanel);

        // Add group panel to tab control
        groupPanel = new JPanel(new BorderLayout());
        JPanel groupNamePanel = new JPanel(new FlowLayout());
        groupNameField = new JTextField(20);
        createGroupButton = new JButton("Create Group");
        createGroupButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                createNewGroup();
            }
        });

        // Add group name field and create group button to group panel
        groupNamePanel.add(new JLabel("Group Name:"));
        groupNamePanel.add(groupNameField);
        groupNamePanel.add(createGroupButton);
        groupPanel.add(groupNamePanel, BorderLayout.NORTH);

        // Add group and participant lists to group panel
        JPanel groupListsPanel = new JPanel(new GridLayout(1, 2));
        JScrollPane groupListScrollPane = new JScrollPane(groupList);
        JScrollPane participantListScrollPane = new JScrollPane(participantList);
        groupListsPanel.add(groupListScrollPane);
        groupListsPanel.add(participantListScrollPane);
        groupPanel.add(groupListsPanel, BorderLayout.CENTER);

        // Add start chat button to group panel
        startChatButton = new JButton("Start Chat");
        startChatButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                startGroupChat();
            }
        });
        groupPanel.add(startChatButton, BorderLayout.SOUTH);

        // Add group panel to tab control
        tabbedPane.addTab("Groups", groupPanel);

        // Add online participants panel to tab control
        JPanel onlineParticipantsPanel = new JPanel(new BorderLayout());
        JPanel checkOnlinePanel = new JPanel(new FlowLayout());
        checkOnlineButton = new JButton("Check Online Participants");
        checkOnlineButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                checkOnlineParticipants();
            }
        });
        checkOnlinePanel.add(checkOnlineButton);
        onlineParticipantsPanel.add(checkOnlinePanel, BorderLayout.NORTH);

        onlineParticipantsListModel = new DefaultListModel<>();
        onlineParticipantsList = new JList<>(onlineParticipantsListModel);
        onlineParticipantsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane onlineParticipantsScrollPane = new JScrollPane(onlineParticipantsList);
        onlineParticipantsPanel.add(onlineParticipantsScrollPane, BorderLayout.CENTER);

        tabbedPane.addTab("Online Participants", onlineParticipantsPanel);

        // Set up window
        setTitle("Text Messaging App - " + username);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(500, 500);
        setVisible(true);

        // Start the message receiving thread
        new Thread(new MessageReceiver()).start();
    }

    /**
     * Sends a message to the server
     */
    private void sendMessage() {
        String message = messageField.getText();
        if (!message.isEmpty()) {
            out.println("MESSAGE " + username + " " + message);
            messageField.setText("");
        }
    }

    /**
     * Creates a new group
     */
    private void createNewGroup() {
        String groupName = groupNameField.getText();
        if (!groupName.isEmpty()) {
            List<String> participants = participantList.getSelectedValuesList();
            if (!participants.isEmpty()) {
                out.println("CREATE_GROUP " + groupName + " " + participants.toString());
                groupListModel.addElement(groupName);
                groupNameField.setText("");
                participantListModel.clear();
            }
        }
    }

    /**
     * Starts a group chat
     */
    private void startGroupChat() {
        String groupName = groupList.getSelectedValue();
        if (groupName != null) {
            out.println("START_GROUP_CHAT " + groupName);
        }
    }

    /**
     * Checks which participants are currently online
     */
    private void checkOnlineParticipants() {
        out.println("CHECK_ONLINE_PARTICIPANTS");
    }

    private class MessageReceiver implements Runnable {
        @Override
        public void run() {
            try {
                String message;
                while ((message = in.readLine()) != null) {
                    final String receivedMessage = message; // Create a final copy of the message
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            chatArea.append(receivedMessage + "\n");
                        }
                    });
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new TextMessagingAppSecondClient();
            }
        });
    }
}
