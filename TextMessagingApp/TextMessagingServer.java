import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TextMessagingServer {

    private static final int PORT = 1234;
    private List<ClientHandler> clients = new ArrayList<>();
    private Map<String, List<ClientHandler>> groups = new HashMap<>();
    private Map<String, ClientHandler> usernames = new HashMap<>();

    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server started on port " + PORT);
            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("Client connected from " + socket.getInetAddress().getHostAddress());
                ClientHandler clientHandler = new ClientHandler(socket);
                clients.add(clientHandler);
                new Thread(clientHandler).start();
            }
        } catch (IOException ex) {
            System.err.println("Error starting server: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    private class ClientHandler implements Runnable {

        private Socket socket;
        private BufferedReader input;
        private OutputStream output;
        private String username;
        private boolean isLoggedOut = false;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                output = socket.getOutputStream();

                // Get the username from the client
                username = input.readLine();
                System.out.println("Client " + username + " connected from " + socket.getInetAddress().getHostAddress());
                usernames.put(username, this);

                // Broadcast a message to all clients to inform them of the new user
                String message = "User " + username + " has joined the chat.";
                System.out.println(message);
                broadcastMessage(message);

                while (!isLoggedOut) {
                    String messageReceived = input.readLine();
                    if (messageReceived == null) {
                        break;
                    }
                    System.out.println("Received message from " + username + ": " + messageReceived);

                    if (messageReceived.startsWith("CREATE_GROUP ")) {
                        // Create a new group chat
                        String groupName = messageReceived.substring(13).trim();
                        createGroupChat(groupName);
                    } else if (messageReceived.startsWith("JOIN_GROUP ")) {
                        // Join an existing group chat
                        String groupName = messageReceived.substring(11).trim();
                        joinGroupChat(groupName);
                    } else if (messageReceived.startsWith("LEAVE_GROUP ")) {
                        // Leave a group chat
                        String groupName = messageReceived.substring(12).trim();
                        leaveGroupChat(groupName);
                    } else if (messageReceived.startsWith("MESSAGE ")) {
                        // Send a message to all clients
                        String chatMessage = messageReceived.substring(8);
                        broadcastMessage(username + ": " + message);
                    } else if (messageReceived.equals("LOGOUT")) {
                        // Log out the user
                        logout();
                    }
                }
            } catch (IOException ex) {
                System.err.println("Error handling client: " + ex.getMessage());
                ex.printStackTrace();
            } finally {
                try {
                    socket.close();
                } catch (IOException ex) {
                    System.err.println("Error closing socket: " + ex.getMessage());
                    ex.printStackTrace();
                }
            }
        }

        private void createGroupChat(String groupName) throws IOException {
            if (groups.containsKey(groupName)) {
                output.write(("Group chat " + groupName + " already exists.\n").getBytes());
            } else {
                List<ClientHandler> groupMembers = new ArrayList<>();
                groupMembers.add(this); // Add the creator to the group
                groups.put(groupName, groupMembers);
                output.write(("Group chat " + groupName + " created successfully.\n").getBytes());
            }
        }
        
        private void joinGroupChat(String groupName) throws IOException {
            if (groups.containsKey(groupName)) {
                List<ClientHandler> groupMembers = groups.get(groupName);
                if (groupMembers.contains(this)) {
                    output.write(("You are already a member of group chat " + groupName + ".\n").getBytes());
                } else {
                    groupMembers.add(this);
                    output.write(("Joined group chat " + groupName + " successfully.\n").getBytes());
                }
            } else {
                output.write(("Group chat " + groupName + " does not exist.\n").getBytes());
            }
        }
        
        private void leaveGroupChat(String groupName) throws IOException {
            if (groups.containsKey(groupName)) {
                List<ClientHandler> groupMembers = groups.get(groupName);
                if (groupMembers.contains(this)) {
                    groupMembers.remove(this);
                    output.write(("Left group chat " + groupName + " successfully.\n").getBytes());
                } else {
                    output.write(("You are not a member of group chat " + groupName + ".\n").getBytes());
                }
            } else {
                output.write(("Group chat " + groupName + " does not exist.\n").getBytes());
            }
        }
        
        private void broadcastMessage(String message) throws IOException {
            for (ClientHandler client : clients) {
                if (client != this) {
                    client.output.write((message + "\n").getBytes());
                }
            }
        }
        
        private void logout() throws IOException {
            clients.remove(this);
            usernames.remove(username);
            isLoggedOut = true;
            String message = "User " + username + " has logged out.";
            System.out.println(message);
            broadcastMessage(message);
            output.write("LOGOUT\n".getBytes());
        }
    }
        
    public static void main(String[] args) {
            TextMessagingServer server = new TextMessagingServer();
            server.start();
        }
}      
               
