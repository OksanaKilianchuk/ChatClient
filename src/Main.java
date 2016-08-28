import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;
import java.util.Scanner;

class GetThread extends Thread {
    private int n;
    private User user;

    GetThread(User user) {
        this.user = user;
    }

    @Override
    public void run() {
        try {
            while (!isInterrupted()) {
                URL url = new URL("http://localhost:8080/get?from=" + n);
                HttpURLConnection http = (HttpURLConnection) url.openConnection();

                try (InputStream is = http.getInputStream()) {
                    int sz = is.available();
                    if (sz > 0) {

                        Gson gson = new GsonBuilder().create();
                        Message[] list = gson.fromJson(new BufferedReader(new InputStreamReader(is)), Message[].class);

                        for (Message m : list) {
                            if (!m.getIsPrivate())
                                System.out.println(m);
                            else {
                                if (m.getTo().equals(user.getLogin()))
                                    System.out.println(m);
                            }
                            n++;
                        }
                    }
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            return;
        }
    }
}

public class Main {

    static Scanner scanner = new Scanner(System.in);
    static User user;

    public static void main(String[] args) {
        while (true) {
            user = autorization();

            GetThread th = new GetThread(user);
            th.setDaemon(true);
            th.start();

            while (true) {

                System.out.println();
                System.out.println("Chat -> enter - 'CHAT'");
                System.out.println("Active users -> enter - 'USERS'");
                System.out.println("Private message -> enter - 'PRIVATE'");
                System.out.println("Chat in room -> enter - 'ROOM'");
                System.out.println("User's status -> enter - 'STATUS'");
                System.out.println("Log out -> enter - 'EXIT'");

                System.out.println();
                String action = scanner.nextLine();
                switch (action) {
                    case "CHAT": {
                        chat("all", "http://localhost:8080/add");
                        break;
                    }
                    case "USERS": {
                        getUsersList();
                        break;
                    }
                    case "PRIVATE": {
                        privateMessage();
                        break;
                    }
                    case "ROOM": {
                        roomChat();
                        break;
                    }
                    case "STATUS": {
                        userStatus();
                        break;
                    }
                    case "EXIT": {
                        th.interrupt();
                        userLogOut();
                        break;
                    }
                    default: {
                        System.out.println("Enter key words, please!");
                    }
                }
                if (action.equals("EXIT"))
                    break;

            }

        }
    }

    static void userLogOut() {
        try {
            int res = user.send("http://localhost:8080/logOut");
            if (res != 200) {
                System.out.println("HTTP error: " + res);
                return;
            }
            user = null;
            System.out.println("You are log out");
        } catch (IOException ex) {
            System.out.println("Error: " + ex.getMessage());
            return;
        }
    }

    static void roomChat() {
        System.out.println("Create new romm -> NEW,  Enter to existing -> ENTER");
        String action = scanner.nextLine();
        System.out.println("Write a room name:");
        String roomName = scanner.nextLine();
        switch (action) {
            case "NEW": {
                System.out.println("Enter users (separated ' ')");
                String text = scanner.nextLine();
                Message m = new Message();
                m.setText(text);
                m.setFrom(user.getLogin());
                m.setTo("new " + roomName);
                m.setPrivate(false);
                try {
                    int res = m.send("http://localhost:8080/room");
                    if (res != 200) {
                        System.out.println("HTTP error: " + res);
                        return;
                    }
                    System.out.println("The chat room " + "'" + roomName + "'" + " was created successful.");
                } catch (IOException ex) {
                    System.out.println("Error: " + ex.getMessage());
                    return;
                }
                break;
            }
            case "ENTER": {
                String url = "http://localhost:8080/room";
                chat(roomName, url);
                break;
            }
        }

    }

    static void privateMessage() {
        System.out.println("write a receiver:");
        String receiver = scanner.nextLine();
        System.out.println("write a text:");
        String text = scanner.nextLine();
        Message m = new Message();
        m.setText(text);
        m.setFrom(user.getLogin());
        m.setTo(receiver);
        m.setPrivate(true);
        try {
            int res = m.send("http://localhost:8080/add");
            if (res != 200) {
                System.out.println("HTTP error: " + res);
                return;
            }
        } catch (IOException ex) {
            System.out.println("Error: " + ex.getMessage());
            return;
        }
    }

    static void userStatus() {
        System.out.println("Enter name of user:");
        String name = scanner.nextLine();
        Message msg = new Message();
        msg.setPrivate(true);
        msg.setText(name);
        msg.setFrom(user.getLogin());
        msg.setTo(user.getLogin());
        msg.setDate(new Date());
        try {
            int res = msg.send("http://localhost:8080/status");
            if (res == 200) {
                System.out.println("User " + name + " is online");
                return;
            } else if (res == 202) {
                System.out.println("User " + name + " is offline or not found");
                return;
            } else {
                System.out.println("HTTP error: " + res);
                return;
            }
        } catch (IOException ex) {
            System.out.println("Error: " + ex.getMessage());
            return;
        }
    }

    static void chat(String to, String url) {

        System.out.println("If you want back to menu, write 'MENU'");
        System.out.println("write a text:");
        while (true) {
            String text = scanner.nextLine();
            if (text.isEmpty() || text.equals("MENU"))
                break;
            Message m = new Message();
            m.setText(text);
            m.setFrom(user.getLogin());
            m.setTo(to);
            m.setPrivate(false);

            try {
                int res = m.send(url);
                if (res != 200) {
                    System.out.println("HTTP error: " + res);
                    return;
                }
            } catch (IOException ex) {
                System.out.println("Error: " + ex.getMessage());
                return;
            }
        }

    }

    static void getUsersList() {
        try {
            URL url = new URL("http://localhost:8080/getUsers");
            HttpURLConnection http = (HttpURLConnection) url.openConnection();

            try (InputStream is = http.getInputStream()) {
                int sz = is.available();
                if (sz > 0) {
                    Gson gson = new GsonBuilder().create();
                    User[] list = gson.fromJson(new BufferedReader(new InputStreamReader(is)), User[].class);

                    for (User user : list) {
                        System.out.println(user);
                    }
                }
            }

        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("Back to menu? (press MENU)");
        String back = scanner.nextLine();
    }

    static User autorization() {

        while (true) {
            try {
                System.out.println("Enter login: ");
                String login = scanner.nextLine();
                System.out.println("Enter password:");
                String password = scanner.nextLine();
                User user = new User(login, password);

                int res = user.send("http://localhost:8080/login");
                if (res == 400)
                    System.out.println("Wrong login or password! ");
                else if (res != 200) {
                    System.out.println("HTTP error: " + res);
                } else {
                    System.out.println("Welcome, " + login + "!");
                    return user;
                }

            } catch (IOException ex) {
                System.out.println("Error: " + ex.getMessage());
            }
        }


    }
}
