package de.hm.cs.netze1;

import java.io.File;
import java.io.IOException;

public class Main {
  
  /**
   * Start listening for File-Transmission.
   * args[0] = port
   * args[1] = output file
   * args[2] = error rate
   * args[3] = drop rate
   * args[4] = dupe rate
   * @param args Console params
   */
  public static void main(String[] args) {
    FileReceiver fileReceiver;
    try {
      if (args.length == 2) {
        int port = Integer.parseInt(args[0]);
        File f = new File(args[1]);
        fileReceiver = new FileReceiver(port, f);
      } else if (args.length == 5) {
        int port = Integer.parseInt(args[0]);
        File f = new File(args[1]);
        double errorRate = Double.parseDouble(args[2]);
        double dropRate = Double.parseDouble(args[3]);
        double dupeRate = Double.parseDouble(args[4]);
        fileReceiver = new FaultyFileReceiver(port, f, errorRate, dropRate, dupeRate);
        System.out.println("Faulty mode");
      } else {
        throw new
          IllegalArgumentException("FileReceiver <port> [<errorrate> <droprate> <duperate>]");
      }
      fileReceiver.establishConnection();
      fileReceiver.receiveFile();
    } catch (IOException e) {
      e.printStackTrace();
      return;
    }
  }
  
}
