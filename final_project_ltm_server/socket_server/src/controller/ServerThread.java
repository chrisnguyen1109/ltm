/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package controller;

import java.io.*;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.*;
import model.User;
import view.ServerView;

/**
 *
 * @author HP
 */
public class ServerThread extends Thread {
    Socket socketOfServer;
    BufferedWriter bw;
    BufferedReader br;
    
    public JTextArea tabServer;
    
    String clientName, clientPass, clientRoom, clientChoice;
    Boolean checkInGame;
    public static Hashtable<String, ServerThread> listUser = new Hashtable<>();
    
    public static final String ACCOUNT_USED = "This account is already login in another place! Please using another account";
    public static final String SIGNIN_SUCCESS = "Login successfully!";
    public static final String ACCOUNT_INVALID = "Account or password is incorrect";
    public static final String SIGNUP_SUCCESS = "Sign up successful!";
    public static final String ACCOUNT_EXIST = "This account has been registerd before! Please use another account!";
    public static final String ACCOUNT_NONEXIST = "This account does not exist!";
    public static final String CREATE_ROOM = "CREATED ROOM SUCCESSFULLY";
    public static final String INVALID_ROOM = "There was an error joining the room. Room does not exist or is full!";
    public static final String NOT_READY_ROOM = "The room requires 2 players to start a game!";
    
    SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
    
    boolean isRunning;
    
    UserDatabase userDB;
    
    public ServerThread(Socket socketOfServer) {
        this.socketOfServer = socketOfServer;
        this.bw = null;
        this.br = null;
        
        isRunning = true;
        
        clientName = "";
        clientPass = "";
        clientRoom = "";
        clientChoice = "unknow";
        checkInGame = false;
        
        userDB = new UserDatabase();
        userDB.connect();
    }

    public void appendMessage(String message) {
        tabServer.append(message);
        tabServer.setCaretPosition(tabServer.getText().length() - 1);
    }
    
    public String recieveFromClient() {
        try {
            return br.readLine();
        } catch (IOException ex) {
            System.out.println(clientName+" is disconnected!");
        }
        return null;
    }

    public void sendToClient(String response) {
        try {
            bw.write(response);
            bw.newLine();
            bw.flush();
        } catch (IOException ex) {
            Logger.getLogger(ServerView.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public void sendToSpecificClient(ServerThread socketOfClient, String response) {
        try {
            BufferedWriter writer = socketOfClient.bw;
            writer.write(response);
            writer.newLine();
            writer.flush();
        } catch (IOException ex) {
            Logger.getLogger(ServerView.class.getName()).log(Level.SEVERE, null, ex);
        }
    }    
    
    public void closeServerThread() {
        try {
            isRunning = false;
            if(bw!=null) {
                bw.close();
            }
            if(br!=null) {
                br.close();
            }            
//            socketOfServer.close();
        } catch (IOException ex) {
            Logger.getLogger(ServerView.class.getName()).log(Level.SEVERE, null, ex);
        }
    }    
    
    public String getAllUsers() {
        StringBuffer kq = new StringBuffer();
        String temp = null;
        
        Enumeration<String> keys = listUser.keys();
        if(keys.hasMoreElements()) {
            String str = keys.nextElement();
            kq.append(str);
        }
        
        while(keys.hasMoreElements()) {
            temp = keys.nextElement();
            kq.append("|").append(temp);
        }
        
        return kq.toString();
    }    

    public void clientQuit() {
        if(clientName != null) {
            this.appendMessage("["+sdf.format(new Date())+"] Client \""+clientName+"\" is disconnected!\n");
            if(checkInGame) {
                userDB.updatePoint(clientName, -1);
            }
            else {
                leaveRoom(clientName);
            }
            checkInGame = false;
            listUser.remove(clientName);
            if(listUser.isEmpty()) this.appendMessage("["+sdf.format(new Date())+"] Now there's no one is connecting to server\n");
            notifyToAllUsers("LIST_ONLINE_USERS|"+getAllUsers());
            closeServerThread();
        }
    }

    public void notifyToAllUsers(String message) {
        Enumeration<ServerThread> clients = listUser.elements();
        ServerThread st;
        BufferedWriter writer;
        while(clients.hasMoreElements()) {
            st = clients.nextElement();
            writer = st.bw;

            try {
                writer.write(message);
                writer.newLine();
                writer.flush();
            } catch (IOException ex) {
                Logger.getLogger(ServerView.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
    
    public String getAvailableUsers() {
        StringBuffer kq = new StringBuffer();
        Enumeration<ServerThread> clients = listUser.elements();
        ServerThread st;

        while(clients.hasMoreElements()) {
            st = clients.nextElement();
            if(st.clientRoom.equals("")) {
                kq.append("|").append(st.clientName);
            }
        }
        
        return kq.toString();
    }
    
    public boolean checkValidRoom(String host) {
        if(!listUser.containsKey(host)) {
            return false;
        }
        if(listUser.get(host).clientRoom=="") {
            return false;
        }
        Enumeration<ServerThread> clients = listUser.elements();
        ServerThread st;
        int count = 0;
        while(clients.hasMoreElements() && count < 2) {
            st = clients.nextElement();
            if(st.clientRoom.equals(host)) {
                count++;
            }
        }
        if(count > 1 || count == 0) {
            return false;
        }
        return true;
    }
        
    public void leaveRoom(String requestUser) {
        if(requestUser.equals(clientRoom)) {
            clientRoom = "";
            Enumeration<ServerThread> clients = listUser.elements();
            ServerThread st;  
            while(clients.hasMoreElements()) {
                st = clients.nextElement();
                System.out.println(st.clientRoom);
                if(st.clientRoom.equals(requestUser)) {
                    System.out.println(st.clientName);
                    ServerThread st_receiverUser = listUser.get(st.clientName);
                    sendToSpecificClient(st_receiverUser, "DESTROY_ROOM|");  
                    break;
                }
            }
        }
        else {
            String hostRoom = clientRoom;
            clientRoom = "";
            if(listUser.containsKey(hostRoom)) {
                ServerThread st_receiverUser = listUser.get(hostRoom);
                sendToSpecificClient(st_receiverUser, "LEAVE_ROOM|");                
            }
        }
    }
    
    @Override
    public void run() {
        try {
            bw = new BufferedWriter(new OutputStreamWriter(socketOfServer.getOutputStream()));
            br = new BufferedReader(new InputStreamReader(socketOfServer.getInputStream()));
            
            boolean isUserExist = true;
            String message, sender, receiver, fileName;
            StringBuffer str;
            String cmd, icon;
            StringTokenizer tokenizer;
            while(isRunning) {
                try {
                    message = recieveFromClient();
                    tokenizer = new StringTokenizer(message, "|");
                    cmd = tokenizer.nextToken();  

                    switch (cmd) {

                        case "LIST_ONLINE_USERS":
                            notifyToAllUsers("LIST_ONLINE_USERS|"+getAllUsers());
                            break;
                            
                        case "CHECK_USERNAME":
                            clientName = tokenizer.nextToken();
                            clientPass = tokenizer.nextToken();
                            isUserExist = listUser.containsKey(clientName);
                            
                            if(isUserExist) {
                                sendToClient(ACCOUNT_USED);
                            }
                            else {
                                boolean kq = userDB.checkUsername(clientName, clientPass);
                                if(kq) {
                                    sendToClient(SIGNIN_SUCCESS);
                                    this.appendMessage("["+sdf.format(new Date())+"] Client \""+clientName+"\" is connecting to server\n");
                                    listUser.put(clientName, this);
                                } 
                                else {
                                    sendToClient(ACCOUNT_INVALID);
                                }
                            }
                            break;
                            
                        case "SIGN_UP":
                            String username = tokenizer.nextToken();
                            String password = tokenizer.nextToken();
                            System.out.println("name: "+username+" password: "+password);

                            int kq = userDB.insertUser(new User(username, password, 0, 0, 0));
                            if(kq > 0) {
                                sendToClient(SIGNUP_SUCCESS);
                            }
                            else {
                                sendToClient(ACCOUNT_EXIST);
                            }
                            break;
                        
                        case "PLAYER_RANK":
                            ArrayList<User> playerRank = userDB.getPlayerRank();
                            String response = "PLAYER_RANK";
                            for(User u : playerRank) {
                                response += "|"+u.getUsername()+"/"+u.getScore()+"/"+u.getTotalMatch()+"/"+u.getTotalWin();
                            }
                            sendToClient(response);
                            break;
                            
                        case "PRIVATE_CHAT":
                            String privateSender = tokenizer.nextToken();
                            String privateReceiver = tokenizer.nextToken();
                            String messageContent = message.substring(cmd.length()+privateSender.length()+privateReceiver.length()+3, message.length());
                            
                            ServerThread st_receiver = listUser.get(privateReceiver);
                            if(st_receiver!=null) {
                                sendToSpecificClient(st_receiver, "PRIVATE_CHAT|" + privateSender + "|" + messageContent);

                                System.out.println("[ServerThread] message = "+messageContent);                                
                            }

                            break;                          
                        
                        case "CREATE_ROOM":
                            clientRoom = tokenizer.nextToken();
                            User firstUser = userDB.getInfoUser(clientRoom);
                            if(firstUser!=null) {
                                sendToClient(CREATE_ROOM+"|"+firstUser.getUsername() + "|" + firstUser.getScore() + "|" + firstUser.getTotalMatch() + "|" + firstUser.getTotalWin());
                            }
                            else {
                                sendToClient(ACCOUNT_NONEXIST);
                            }
                            break;
                            
                        case "AVAILABLE_USERS":
                            sendToClient("AVAILABLE_USERS"+"|"+getAvailableUsers());
                            break;
                        
                        case "INVITE_USER":
                            String senderUser = tokenizer.nextToken();
                            String receiverUser = tokenizer.nextToken();
                            
                            ServerThread st_receiverUser = listUser.get(receiverUser);
                            if(st_receiverUser!=null) {
                                sendToSpecificClient(st_receiverUser, "RECEIVE_USER|" + senderUser);                               
                            }

                            break; 
                            
                        case "ACCEPT_SENDER":
                            String host = tokenizer.nextToken();
                            String you = tokenizer.nextToken();
                            ServerThread st_host = listUser.get(host);
                            if(checkValidRoom(host)) {
                                clientRoom = host;
                                User hostUser = userDB.getInfoUser(host);
                                User youUser = userDB.getInfoUser(you);
                                if(hostUser == null || youUser == null) {
                                    sendToClient(ACCOUNT_NONEXIST);
                                }
                                else {
                                    sendToClient("JOIN_ROOM|"+hostUser.getUsername() + "|" + hostUser.getScore() + "|" + hostUser.getTotalMatch() + "|" + hostUser.getTotalWin() + "|"+youUser.getUsername() + "|" + youUser.getScore() + "|" + youUser.getTotalMatch() + "|" + youUser.getTotalWin());
                                    sendToSpecificClient(st_host, "USER_ACCEPT|"+youUser.getUsername() + "|" + youUser.getScore() + "|" + youUser.getTotalMatch() + "|" + youUser.getTotalWin());
                                }                                
                            }
                            else {
                                sendToClient(INVALID_ROOM);
                            }
                            
                            break;

                        case "LEAVE_ROOM_GAME":
                            String requestUser = tokenizer.nextToken();
                            leaveRoom(requestUser);
                            
                            break;
                            
                        case "START_GAME":
                            Enumeration<ServerThread> clients = listUser.elements();
                            ServerThread st;
                            int count = 0;
                            String player2 = "";
                            while(clients.hasMoreElements() && count < 2) {
                                st = clients.nextElement();
                                if(st.clientRoom.equals(clientRoom)) {
                                    count++;
                                    if(!st.clientName.equals(clientRoom)) {
                                        player2 = st.clientName;
                                    }
                                }
                            }
                            if(count == 2) {
                                checkInGame = true;
                                listUser.get(player2).checkInGame = true;
                                ServerThread st_player2 = listUser.get(player2);
                                sendToClient("READY_ROOM|"+player2);
                                sendToSpecificClient(st_player2, "READY_ROOM|"+clientName);
                            }
                            else {
                                sendToClient(NOT_READY_ROOM);
                            }
                            break;
                            
                        case "FINISH_GAME":
                            String opponentPlayer = tokenizer.nextToken();
                            clientChoice = tokenizer.nextToken();
                            try {
                                Thread.sleep(500);
                            } catch (InterruptedException ex) {
                                Logger.getLogger(ServerThread.class.getName()).log(Level.SEVERE, null, ex);
                            }
                            clientRoom = "";
                            String opponentChoice = "unknow";
                            if(listUser.containsKey(opponentPlayer)) {
                                opponentChoice = listUser.get(opponentPlayer).clientChoice;
                            }
                            int point = -100;
                            String result = "";
                            String msg = "";
                            if(clientChoice.equals(opponentChoice)) {
                                point = 0;
                                result = "DRAW";
                                msg = "You get "+point+" point";
                            }
                            else if(clientChoice.equals("unknow")) {
                                point = -1;
                                result = "LOSE";
                                msg = "You get "+point+"point";                                
                            }
                            else if(opponentChoice.equals("unknow")) {
                                point = 1;
                                result = "WIN";
                                msg = "You get "+point+"point";                                
                            }
                            else if(clientChoice.equals("rock") && opponentChoice.equals("scissor")) {
                                point = 1;
                                result = "WIN";
                                msg = "You get "+point+"point";                                
                            }
                            else if(clientChoice.equals("rock") && opponentChoice.equals("paper")) {
                                point = -1;
                                result = "LOSE";
                                msg = "You get "+point+"point";                                
                            }
                            else if(clientChoice.equals("paper") && opponentChoice.equals("rock")) {
                                point = 1;
                                result = "WIN";
                                msg = "You get "+point+"point";                                
                            }
                            else if(clientChoice.equals("paper") && opponentChoice.equals("scissor")) {
                                point = -1;
                                result = "LOSE";
                                msg = "You get "+point+"point";                                
                            }
                            else if(clientChoice.equals("scissor") && opponentChoice.equals("paper")) {
                                point = 1;
                                result = "WIN";
                                msg = "You get "+point+"point";                                
                            }
                            else if(clientChoice.equals("scissor") && opponentChoice.equals("rock")) {
                                point = -1;
                                result = "LOSE";
                                msg = "You get "+point+"point";                                
                            }
                            
                            if(point!=-100 && userDB.updatePoint(clientName, point)) {
                                sendToClient("RESULT_GAME|"+opponentChoice+"|"+result+"|"+msg);
                            }
                            else {
                                sendToClient("FINISH_GAME_ERROR|");
                            }
                            break;
                            
                        default:
//                            notifyToAllUsers(message);
                            break;
                    }
                    
                } catch (Exception e) {
                    clientQuit();
                    break;
                }
            }
        } catch (IOException ex) {
            clientQuit();
            Logger.getLogger(ServerThread.class.getName()).log(Level.SEVERE, null, ex);
        }
    }    
}
