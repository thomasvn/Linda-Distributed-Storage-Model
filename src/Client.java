import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;
import java.util.concurrent.ThreadLocalRandom;


// This class is only used to test the server

public class Client {
    public static void main(String [] args) {

        try {
            // Config Variables
            String host = "129.210.16.80";
            int port = 9998;

            // Print IP Address
            InetAddress ipAddr = InetAddress.getLocalHost();
            System.out.println(ipAddr.getHostAddress());

            // Connect with Host1
            Socket s = new Socket();
            s.connect(new InetSocketAddress(host, port));

            System.out.println("Connection Successful");

            s.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
