package de.hm.cs.netze1;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/*
  Krivoj
 */
public class FileReceiver {

  private static final int PACKET_SIZE = 64000;
  
  private File file;
  private Set<Package> receivedPackages = new HashSet<>();
  private int SYNseq;
  private int FINseq;
  private int bytesReceived;
  
  private DatagramSocket socket;
  // Destination
  private InetAddress destAddress;
  private int destPort;
  
  public FileReceiver(int port, File file) throws IOException {
    this.socket = new DatagramSocket(port);
    this.file = file;
  }
  
  private Package extractPackage(DatagramPacket dp) throws ChecksumException {
    byte[] input = new byte[dp.getLength()];
    System.arraycopy(dp.getData(), dp.getOffset(), input, 0, dp.getLength());
    return new Package(input);
  }
  
  private DatagramPacket acknowledgePacket(Package p) {
    Package ack = new Package();
    ack.setACK(true);
    ack.setAcknowledgementNumber(p.getSequenceNumber());
    byte[] data = ack.makePaket();
    return new DatagramPacket(data, data.length, destAddress, destPort);
  }
  
  protected DatagramPacket recvPacket() throws IOException {
    DatagramPacket packet = new DatagramPacket(new byte[PACKET_SIZE], PACKET_SIZE);
    this.socket.receive(packet);
    return packet;
  }

  // Z1
  public void establishConnection() throws IOException {
    boolean isSyn = false;
    while (!isSyn) {
      DatagramPacket in = recvPacket();
      Package p;
      try {
        p = extractPackage(in);
      } catch (ChecksumException e) {
        continue;
      }
      isSyn = p.isSYN();
      if (isSyn) {
        System.out.println("Received SYN");
        this.destAddress = in.getAddress();
        this.destPort = in.getPort();
        this.socket.send(acknowledgePacket(p));
        this.SYNseq = p.getSequenceNumber();
        this.bytesReceived = this.SYNseq + 1;
      }
    }
  }
  
  public void receiveFile() throws IOException {
    waitForFin();
    System.out.println("Received FIN, wait for remaining packages");
    waitForRemainingPackages();
    System.out.println("Received all packages, start writing file");
    writeFile();
    System.out.println("Wrote file");
  }
  
  // Z2
  private void waitForFin() throws IOException {
    boolean isFin = false;
    Package p = null;
    while (!isFin) {
      try {
        p = extractPackage(recvPacket());
        isFin = p.isFIN();
        if (this.receivedPackages.add(p))
          this.bytesReceived += p.getPayload().length;
        this.socket.send(acknowledgePacket(p));
      } catch (ChecksumException e) {
        continue;
      }
    }

    this.FINseq = p.getSequenceNumber();
  }
  
  // Z3
  private void waitForRemainingPackages() throws IOException {
    System.out.println(receivedPackages.stream().mapToInt(x -> x.getPayload().length).sum());
    while (this.bytesReceived != this.FINseq) {
      System.out.println(bytesReceived + " | " + this.FINseq);
      try {
        Package p = extractPackage(recvPacket());
        if (this.receivedPackages.add(p))
          this.bytesReceived += p.getPayload().length;
        this.socket.send(acknowledgePacket(p));
      } catch (ChecksumException e) {
        continue;
      }
    }
  }
  
  private void writeFile() {
    try (FileOutputStream fos = new FileOutputStream(file)) {
      Map<Integer, byte[]> payloads = receivedPackages.stream()
          .collect(Collectors.toMap(x -> x.getSequenceNumber(), x -> x.getPayload()));
      int bytesWritten = this.SYNseq + 1;
      while (bytesWritten != this.bytesReceived - 1) {
        System.out.println(bytesWritten);
        fos.write(payloads.get(bytesWritten));
        bytesWritten += payloads.get(bytesWritten).length;
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
  
  private int currentSize() {
    return receivedPackages.stream().mapToInt(p -> p.getPayload().length).sum();
  }
  
}
