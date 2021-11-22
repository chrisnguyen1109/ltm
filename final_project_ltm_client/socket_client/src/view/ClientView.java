/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package view;

import java.awt.Color;
import java.awt.event.*;
import java.io.*;
import java.net.Socket;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

/**
 *
 * @author HP
 */
public class ClientView extends JFrame implements Runnable {
    public static final String HOSTNAME = "localhost";
    private static int PORT = 1109;
    public static final String ACCOUNT_USED = "This account is already login in another place! Please using another account";
    public static final String SIGNIN_SUCCESS = "Login successfully!";
    public static final String ACCOUNT_INVALID = "Account or password is incorrect";
    public static final String SIGNUP_SUCCESS = "Sign up successful!";
    public static final String ACCOUNT_EXIST = "This account has been registerd before! Please use another account!";
    public static final String ACCOUNT_NONEXIST = "This account does not exist!";
    public static final String CREATE_ROOM = "CREATED ROOM SUCCESSFULLY";
    public static final String INVALID_ROOM = "There was an error joining the room. Room does not exist or is full!";
    public static final String NOT_READY_ROOM = "The room requires 2 players to start a game!";
    
    String username;
    String onlineUser;
    Socket socketOfClient;
    BufferedWriter bw;
    BufferedReader br;
    
    JPanel mainPanel;
    LoginPanel loginPanel;
    WelcomePanel welcomePanel;
    SignUpPanel signUpPanel;
    HomePanel homePanel;
    RoomPanel roomPanel;
    GamePanel gamePanel;
    RankPanel rankPanel;
    
    Thread clientThread;
    boolean isRunning;
    
    JMenuBar menuBar;
    JMenu menuAccount;
    JMenuItem itemChangePass, itemLogout;
    
    
    StringTokenizer tokenizer;
    
    Socket socketOfSender, socketOfReceiver;
    
    DefaultListModel<String> listModel;
    DefaultListModel<String> listAvailableModel;
    Hashtable<String, ChatFrame> listReceiver;
    DefaultTableModel rankDefaultTableModel;
        
    boolean isConnectToServer;
    
    int timeClicked = 0;
    int countDown = 15;
    String yourChoice = "unknow";
    String opponentPlayer = "";

    public ClientView(String username) {
        this.username = username;
        socketOfClient = null;
        bw = null;
        br = null;
        isRunning = true;
        listModel = new DefaultListModel<>();
        listAvailableModel = new DefaultListModel<>();
        isConnectToServer = false;
        listReceiver = new Hashtable<>();
        rankDefaultTableModel = new DefaultTableModel();
        
        mainPanel = new JPanel();
        loginPanel = new LoginPanel();
        welcomePanel = new WelcomePanel();
        signUpPanel = new SignUpPanel();
        homePanel = new HomePanel();
        roomPanel = new RoomPanel();
        gamePanel = new GamePanel();
        rankPanel = new RankPanel();
        
        welcomePanel.setVisible(true);
        signUpPanel.setVisible(false);
        loginPanel.setVisible(false);
        homePanel.setVisible(false);
        roomPanel.setVisible(false);
        gamePanel.setVisible(false);
        rankPanel.setVisible(false);
        
        mainPanel.add(welcomePanel);
        mainPanel.add(signUpPanel);
        mainPanel.add(loginPanel);
        mainPanel.add(homePanel);
        mainPanel.add(roomPanel);
        mainPanel.add(gamePanel);
        mainPanel.add(rankPanel);
        
        addEventsForWelcomePanel();
        addEventsForSignUpPanel();
        addEventsForLoginPanel();
        addEventsForHomePanel();
        addEventsForRoomPanel();
        addEventsForGamePanel();
        addEventsForRankPanel();
        
        menuBar = new JMenuBar();
        menuAccount = new JMenu();
        itemLogout = new JMenuItem();
        itemChangePass = new JMenuItem();
        
        menuAccount.setText("Account");
        itemLogout.setText("Logout");
        itemChangePass.setText("Change password");
//        menuAccount.add(itemChangePass);
        menuAccount.add(itemLogout);
        
        menuBar.add(menuAccount); 
        
        itemChangePass.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {

            }
        });
        itemLogout.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                int kq = JOptionPane.showConfirmDialog(ClientView.this, "Are you sure to logout?", "Notice", JOptionPane.YES_NO_OPTION);
                if(kq == JOptionPane.YES_OPTION) {
                    ClientView.this.disconnect();
                }
            }
        });

        menuBar.setVisible(false);
        
        setJMenuBar(menuBar);
        pack();
        mainPanel.setBackground(Color.WHITE);
        add(mainPanel);
        setSize(570, 520);
        setLocation(400, 100);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setTitle(username);        
    }
    
    private void addEventsForWelcomePanel() {
        
        welcomePanel.getBtnLogin().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                welcomePanel.setVisible(false);
                signUpPanel.setVisible(false);
                loginPanel.setVisible(true);
            }
        });
        welcomePanel.getBtnSignUp().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                welcomePanel.setVisible(false);
                signUpPanel.setVisible(true);
                loginPanel.setVisible(false);
            }
        });
        
    }    

    private void addEventsForSignUpPanel() {
        signUpPanel.getBtnBack().addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent me) {
                signUpPanel.clearTf();
                welcomePanel.setVisible(true);
                signUpPanel.setVisible(false);
                loginPanel.setVisible(false);
            }
        });
        signUpPanel.getBtnSignUp().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                btnSignUpEvent();
            }
        });
    }

    private void addEventsForLoginPanel() {
        loginPanel.getBtnOK().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                btnOkEvent();
            }
        });
        loginPanel.getBtnBack().addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent me) {
                loginPanel.clearTf();
                welcomePanel.setVisible(true);
                signUpPanel.setVisible(false);
                loginPanel.setVisible(false);
            }
        });
    }
    
    private void addEventsForHomePanel() {
        homePanel.getBtnCreatRoom().addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent me) {
                openCreateRoom();
            }
        });
        
        homePanel.getBtnJoinRoom().addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent me) {
                String room = JOptionPane.showInputDialog(new JFrame(), "Enter Room Id:"); 
                if(room!=null) {
                    ClientView.this.sendToServer("ACCEPT_SENDER|"+room+"|"+ClientView.this.username);                    
                }
            }
        });

        homePanel.getBtnRank().addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent me) {
                ClientView.this.sendToServer("PLAYER_RANK|"+null); 
            }
        });        
        
        homePanel.getOnlineList().addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent me) {
                openPrivateChatOutsideRoom("home");
            }
        });
    }
    
    private void addEventsForRoomPanel() {
        
        roomPanel.btnStart().addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent me) {
                startGame();
            }
         });
        
        roomPanel.btnInvite().addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent me) {
                inviteRoom();
            }
        });

        roomPanel.btnLeave().addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent me) {
               leaveRoom();
            }
        });     
        
        roomPanel.getOnlineListWR().addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent me) {
                openPrivateChatOutsideRoom("room");
            }
        });        
    }
    
    private void addEventsForGamePanel() {
        
        gamePanel.getRock().addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent me) {
                ClientView.this.gamePanel.getYourChoice().setText("Your choice: 'Rock'");
                ClientView.this.yourChoice = "rock";
            }
        });
        
        gamePanel.getPaper().addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent me) {
                ClientView.this.gamePanel.getYourChoice().setText("Your choice: 'Paper'");
                ClientView.this.yourChoice = "paper";
            }
        });

        gamePanel.getScissor().addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent me) {
                ClientView.this.gamePanel.getYourChoice().setText("Your choice: 'Scissor'");
                ClientView.this.yourChoice = "scissor";
            }
        });        
    }
    
    private void addEventsForRankPanel() {
        rankPanel.getBtnBack().addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent me) {
                rankPanel.setVisible(false);
                homePanel.setVisible(true);
            }
        });        
    }

    private void openCreateRoom() {          
        this.sendToServer("CREATE_ROOM|"+this.username);                
    }
    
    private void inviteRoom() {          
        this.sendToServer("AVAILABLE_USERS|"+null);
    }    
    
    private void leaveRoom() {   
        int kq = JOptionPane.showConfirmDialog(ClientView.this, "Are you sure to leave the room?", "Notice", JOptionPane.YES_NO_OPTION);
        if(kq == JOptionPane.YES_OPTION) {
            this.sendToServer("LEAVE_ROOM_GAME|"+this.username);  
            this.roomPanel.setVisible(false);
            this.homePanel.setVisible(true); 
        }               
    }
    
    private void startGame() {
        int kq = JOptionPane.showConfirmDialog(ClientView.this, "Are you sure to start the game?", "Notice", JOptionPane.YES_NO_OPTION);
        if(kq == JOptionPane.YES_OPTION) {
            this.sendToServer("START_GAME|");
        }           
        
    }
    
    private void openPrivateChatOutsideRoom(String place) {
        timeClicked++;
        if(timeClicked == 1) {
            Thread countingTo500ms = new Thread(counting);
            countingTo500ms.start();
        }

        if(timeClicked == 2) {
            String privateReceiver = place.equals("home") ? homePanel.getOnlineList().getSelectedValue() : roomPanel.getOnlineListWR().getSelectedValue();
            ChatFrame cf = listReceiver.get(privateReceiver);
            if(cf == null) {
                cf = new ChatFrame(username, privateReceiver, bw, br);
                
                cf.getLbReceiver().setText("Private chat with \""+cf.receiver+"\"");
                cf.setTitle(cf.sender);
                cf.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                cf.setVisible(true);

                listReceiver.put(privateReceiver, cf);
            } else {
                cf.setVisible(true);
            }
        }
    }
    
    private void finishGame() {
        Thread counting1000ms = new Thread(countDown15s);
        counting1000ms.start();
    }

    Runnable counting = new Runnable() {
        @Override
        public void run() {
            try {
                Thread.sleep(500);
            } catch (InterruptedException ex) {
                Logger.getLogger(ClientView.class.getName()).log(Level.SEVERE, null, ex);
            }
            timeClicked = 0;
        }
    }; 
    
    Runnable countDown15s = new Runnable() {
        @Override
        public void run() {
            ClientView.this.gamePanel.getCounter().setText(countDown + "s");
            while(countDown > 0) {
                try {
                    Thread.sleep(1000);
                    countDown--;
                    ClientView.this.gamePanel.getCounter().setText(countDown + "s");
                } catch (InterruptedException ex) {
                    Logger.getLogger(ClientView.class.getName()).log(Level.SEVERE, null, ex);
                }                
            }
            sendToServer("FINISH_GAME|"+ClientView.this.opponentPlayer+"|"+ClientView.this.yourChoice);
            opponentPlayer = "";
            yourChoice = "unknow";
            countDown = 15;
        }
    };    

    private void btnSignUpEvent() {
        String password = this.signUpPanel.getTfPassword().getText();
        String confirmPassword = this.signUpPanel.getTfConfirmPassword().getText();
        if(!password.equals(confirmPassword)) {
            JOptionPane.showMessageDialog(this, "Password confirmation doesn't match password", "Error", JOptionPane.ERROR_MESSAGE);
        } else {
            String username = signUpPanel.getTfUsername().getText().trim();
            if(username.equals("") || password.equals("") || confirmPassword.equals("")) {
                JOptionPane.showMessageDialog(this, "Please fill up all fields", "Notice!", JOptionPane.WARNING_MESSAGE);
                return;
            }
            if(!isConnectToServer) {
                isConnectToServer = true;
                this.connectToServer(HOSTNAME);
            }    
            this.sendToServer("SIGN_UP|" +username+"|"+password);
        
            String response = this.recieveFromServer();
            if(response != null) {
                if(response.equals(ACCOUNT_EXIST)) {                   
                    JOptionPane.showMessageDialog(this, response, "Error", JOptionPane.ERROR_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(this, response+"\nYou can now go back and login to join the game", "Success!", JOptionPane.INFORMATION_MESSAGE);
                    signUpPanel.clearTf();
                    welcomePanel.setVisible(true);
                    signUpPanel.setVisible(false);
                    loginPanel.setVisible(false);
                }
            }
        }
        
    }
    
    private void btnOkEvent() {
        String username = loginPanel.getTfUsername().getText().trim();
        String password = loginPanel.getTfPassword().getText().trim();
        
        this.username = username;
        
        if(username.equals("") || password.equals("")) {
            JOptionPane.showMessageDialog(this, "Please fill up all fields", "Notice!", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if(!isConnectToServer) {
            isConnectToServer = true;
            this.connectToServer(HOSTNAME);
        }    
        this.sendToServer("CHECK_USERNAME|" +this.username+"|"+password);
        
        String response = this.recieveFromServer();
        if(response != null) {
            if (response.equals(ACCOUNT_USED) || response.equals(ACCOUNT_INVALID)) {
                JOptionPane.showMessageDialog(this, response, "Error", JOptionPane.ERROR_MESSAGE);
            } 
            else {
                loginPanel.setVisible(false);
                homePanel.setVisible(true);
                this.setTitle("\""+username+"\"");

                menuBar.setVisible(true);

                clientThread = new Thread(this);
                clientThread.start();
                this.sendToServer("LIST_ONLINE_USERS|"+this.onlineUser);

                System.out.println("this is \""+username+"\"");
            }
        } 
        else {
            System.out.println("[btOkEvent()] Server is not open yet, or already closed!");
        }
    }    

    public void connectToServer(String hostAddress) {
        try {
            socketOfClient = new Socket(hostAddress, PORT);
            bw = new BufferedWriter(new OutputStreamWriter(socketOfClient.getOutputStream()));
            br = new BufferedReader(new InputStreamReader(socketOfClient.getInputStream()));
            
        } catch (java.net.UnknownHostException e) {
            isConnectToServer = false;
            JOptionPane.showMessageDialog(this, "Host IP is not correct.\nPlease try again!", "Failed to connect to server", JOptionPane.ERROR_MESSAGE);
        } catch (java.net.ConnectException e) {
            isConnectToServer = false;
            JOptionPane.showMessageDialog(this, "Server is unreachable, maybe server is not open yet, or can't find this host.\nPlease try again!", "Failed to connect to server", JOptionPane.ERROR_MESSAGE);
        } catch(java.net.NoRouteToHostException e) {
            isConnectToServer = false;
            JOptionPane.showMessageDialog(this, "Can't find this host!\nPlease try again!", "Failed to connect to server", JOptionPane.ERROR_MESSAGE);
        } catch (IOException ex) {
            Logger.getLogger(ClientView.class.getName()).log(Level.SEVERE, null, ex);
            
        }
    }

    public void sendToServer(String line) {
        try {
            this.bw.write(line);
            this.bw.newLine();
            this.bw.flush();
        } catch (java.net.SocketException e) {
            isConnectToServer = false;
            JOptionPane.showMessageDialog(this, "Server is closed, can't send message!", "Error", JOptionPane.ERROR_MESSAGE);
        } catch (java.lang.NullPointerException e) {
            isConnectToServer = false;
            System.out.println("[sendToServer()] Server is not open yet, or already closed!");
        } catch (IOException ex) {
            Logger.getLogger(ClientView.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public String recieveFromServer() {
        try {
            return this.br.readLine();
        } catch (java.lang.NullPointerException e) {
            isConnectToServer = false;
            System.out.println("[recieveFromServer()] Server is not open yet, or already closed!");
        } catch (IOException ex) {
            System.out.println("[recieveFromServer()] Socket client is closed!");
//            Logger.getLogger(ClientView.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    public void disconnect() {
        try {
            isConnectToServer = false;
            isRunning = false;             
            this.bw.close();   
            this.br.close();
            this.socketOfClient.close();        
            ClientView.this.setVisible(false);
            new ClientView(null).setVisible(true);            
        } catch (IOException ex) {
            Logger.getLogger(ClientView.class.getName()).log(Level.SEVERE, null, ex);
        }
    }    

    @Override
    public void run() {
        String response;
        String sender;
        String msg;
        String cmd;
        ChatFrame cf;
        while(isRunning) {
            response = this.recieveFromServer();
            cmd = "";
            if(response!=null) {
                tokenizer = new StringTokenizer(response, "|");
                cmd = tokenizer.nextToken();   
                System.out.println(cmd);
            }
            switch (cmd) {
                case "LIST_ONLINE_USERS":
                    listModel.clear();
                    while(tokenizer.hasMoreTokens()) {
                        cmd = tokenizer.nextToken();
                        listModel.addElement(cmd);
                    }

                    listModel.removeElement(this.username);
                    homePanel.getOnlineList().setModel(listModel);
                    roomPanel.getOnlineListWR().setModel(listModel);
                    break;

                case "PRIVATE_CHAT":
                    sender = tokenizer.nextToken();
                    msg = response.substring(cmd.length()+sender.length()+2, response.length());
                    
                    cf = listReceiver.get(sender);
                    
                    if(cf == null) {
                        cf = new ChatFrame(username, sender, bw, br);
                        cf.sender = username;
                        cf.receiver = sender;
                        cf.bw = ClientView.this.bw;
                        cf.br = ClientView.this.br;

                        cf.getLbReceiver().setText("Private chat with \""+cf.receiver+"\"");
                        cf.setTitle(cf.sender);
                        cf.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                        cf.setVisible(true);

                        listReceiver.put(sender, cf);
                    } else {
                        cf.setVisible(true);
                    }
                    cf.appendMessage_Left(sender+": ", msg);
                    break;  
                    
                case CREATE_ROOM:
                    this.roomPanel.getRoomId().setText("Room Id: "+this.username);
                    this.roomPanel.getPlayer1().setText(tokenizer.nextToken());
                    this.roomPanel.getPlayer2().setText("Unknow");
                    this.roomPanel.getScore1().setText("Score: " + tokenizer.nextToken());
                    this.roomPanel.getScore2().setText("");
                    this.roomPanel.getTotalMatch1().setText("Total Match: " + tokenizer.nextToken());
                    this.roomPanel.getTotalMatch2().setText("");
                    this.roomPanel.getTotalWin1().setText("Total Win: " + tokenizer.nextToken());
                    this.roomPanel.getTotalWin2().setText("");
                    this.roomPanel.btnStart().setVisible(true);
                    this.roomPanel.btnInvite().setVisible(true);                    
                    this.homePanel.setVisible(false);
                    this.roomPanel.setVisible(true);   
                    break;
                
                case ACCOUNT_NONEXIST:
                    JOptionPane.showMessageDialog(this, "Unknow Error", "Error", JOptionPane.ERROR_MESSAGE);
                    break;
                    
                case "AVAILABLE_USERS":
                    listAvailableModel.clear();
                    while(tokenizer.hasMoreTokens()) {
                        cmd = tokenizer.nextToken();
                        listAvailableModel.addElement(cmd);
                    }
                    OnlineFrame of = new OnlineFrame(username, bw, br);
                    of.setTitle(of.sender);
                    of.getAvailableList().setModel(listAvailableModel);
                    of.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                    of.setVisible(true);                    
                    break; 
                    
                case "RECEIVE_USER":
                    sender = tokenizer.nextToken();
                    int kq = JOptionPane.showConfirmDialog(ClientView.this, "An user named "+ sender +" invites you to the game?", "Notice", JOptionPane.YES_NO_OPTION);
                    if(kq == JOptionPane.YES_OPTION) {
                        this.sendToServer("ACCEPT_SENDER|"+sender+"|"+this.username);
                    }
                    break;
                
                case "JOIN_ROOM":
                    String host = tokenizer.nextToken();
                    this.roomPanel.getRoomId().setText("Room Id: "+host);
                    this.roomPanel.getPlayer1().setText(host);
                    this.roomPanel.getScore1().setText("Score: " + tokenizer.nextToken());
                    this.roomPanel.getTotalMatch1().setText("Total Match: " + tokenizer.nextToken());
                    this.roomPanel.getTotalWin1().setText("Total Win: " + tokenizer.nextToken());
                    this.roomPanel.getPlayer2().setText(tokenizer.nextToken());
                    this.roomPanel.getScore2().setText("Score: " + tokenizer.nextToken());
                    this.roomPanel.getTotalMatch2().setText("Total Match: " + tokenizer.nextToken());
                    this.roomPanel.getTotalWin2().setText("Total Win: " + tokenizer.nextToken());
                    this.roomPanel.btnStart().setVisible(false);
                    this.roomPanel.btnInvite().setVisible(false);
                    this.homePanel.setVisible(false);
                    this.roomPanel.setVisible(true);                       
                    break;

                case "USER_ACCEPT":
                    this.roomPanel.getPlayer2().setText(tokenizer.nextToken());
                    this.roomPanel.getScore2().setText("Score: " + tokenizer.nextToken());
                    this.roomPanel.getTotalMatch2().setText("Total Match: " + tokenizer.nextToken());
                    this.roomPanel.getTotalWin2().setText("Total Win: " + tokenizer.nextToken());                  
                    break;

                case INVALID_ROOM:
                    JOptionPane.showMessageDialog(this, response, "Error", JOptionPane.ERROR_MESSAGE);
                    break; 
                
                case "LEAVE_ROOM":
                    this.roomPanel.getPlayer2().setText("Unknow");
                    this.roomPanel.getScore2().setText("");
                    this.roomPanel.getTotalMatch2().setText("");
                    this.roomPanel.getTotalWin2().setText("");                    
                    break;
                    
                case "DESTROY_ROOM":
                    JOptionPane.showMessageDialog(this, "The room was canceled", "Error", JOptionPane.ERROR_MESSAGE);
                    this.roomPanel.setVisible(false);
                    this.homePanel.setVisible(true);
                    break;  
                    
                case "READY_ROOM":
                    String opponent = tokenizer.nextToken();
                    this.gamePanel.getOpponentChoice().setText(opponent+" choosing...");
                    this.gamePanel.getYourChoice().setText("Your choice:");
                    this.roomPanel.setVisible(false);
                    this.gamePanel.setVisible(true);
                    opponentPlayer = opponent;
                    finishGame();
                    break;
                    
                case NOT_READY_ROOM:
                    JOptionPane.showMessageDialog(this, response, "Error", JOptionPane.ERROR_MESSAGE);
                    break;    
                    
                case "RESULT_GAME":
                    this.gamePanel.getOpponentChoice().setText("Opponent's choice: "+tokenizer.nextToken());
                    String resultNotif = tokenizer.nextToken();
                    String message = tokenizer.nextToken();
                    JOptionPane.showMessageDialog(this, message, resultNotif, JOptionPane.INFORMATION_MESSAGE);
                    this.gamePanel.setVisible(false);
                    this.homePanel.setVisible(true);
                    break; 
                    
                case "FINISH_GAME_ERROR":
                    JOptionPane.showMessageDialog(this, "Unknow Error", "Error", JOptionPane.ERROR_MESSAGE);
                    this.gamePanel.setVisible(false);
                    this.homePanel.setVisible(true);                    
                    break;
                
                case "PLAYER_RANK":
                    rankDefaultTableModel = (DefaultTableModel) rankPanel.getTblRank().getModel();
                    rankDefaultTableModel.setRowCount(0);
                    int count = 1;
                    while(tokenizer.hasMoreTokens()) {
                        StringTokenizer data = new StringTokenizer(tokenizer.nextToken(), "/"); 
                        rankDefaultTableModel.addRow(new Object[]{count++, data.nextToken(), data.nextToken(), data.nextToken()});
                        homePanel.setVisible(false);
                        rankPanel.setVisible(true);
                    }                 
                    break;
                    
                case "SERVER_CLOSE":
                    JOptionPane.showMessageDialog(this, "Server is closed", "Error", JOptionPane.ERROR_MESSAGE);
                    Enumeration<String> e = listReceiver.keys();
                    while (e.hasMoreElements()) {
                        listReceiver.get(e.nextElement()).dispose();
                    }                    
                    this.disconnect();
                    break;                    
                
                default:
                    break;

            }
        }
        System.out.println("Disconnected to server!");            
    }
    
}


