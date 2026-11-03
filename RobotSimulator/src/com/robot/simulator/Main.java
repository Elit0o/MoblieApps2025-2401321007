package com.robot.simulator;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

public class Main {

    public static void main(String[] args) {
        int portNumber = 8080;
        
        System.out.println("Starting robot server...");
        
        try {
            ServerSocket serverSocket = new ServerSocket(portNumber);
            System.out.println("Server is listening on port " + portNumber);
            
            while (true) {
                System.out.println("Waiting for phone connection...");
                Socket phoneSocket = serverSocket.accept();
                System.out.println("Success! A phone connected to the server.");
                
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(phoneSocket.getInputStream())
                );
                
                String incomingMessage;
                while ((incomingMessage = reader.readLine()) != null) {
                    System.out.println("Received from phone: " + incomingMessage);
                }
                
                phoneSocket.close();
                System.out.println("Phone disconnected. Ready for next command.\n");
            }
            
        } catch (IOException e) {
            System.out.println("An error occurred: " + e.getMessage());
        }
    }
}