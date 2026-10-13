package com.robot.simulator;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class Main {

	public static void main(String[] args) {
		int portNumber = 8080;
		
		System.out.println("Starting robot server...");
		
		try {
			ServerSocket serverSocket = new ServerSocket(portNumber);
			System.out.println("Server is listening on port " + portNumber);
			
			Socket phoneSocket = serverSocket.accept();
			System.out.println("Success! A phone connected to the server.");
		}
		catch(IOException e){
			System.out.println("An error occurred: " + e.getMessage());
		}
	}

}
