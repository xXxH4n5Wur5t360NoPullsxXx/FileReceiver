package de.hm.cs.netze1;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class FileReceiver {

  private static final int PACKET_SIZE = 64000;
  
  private File file;
  private Set<Package> receivedPackages = new HashSet<>();
  
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
        this.receivedPackages.add(p);
        this.destAddress = in.getAddress();
        this.destPort = in.getPort();
        this.socket.send(acknowledgePacket(p));
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
    while (!isFin) {
      try {
        Package p = extractPackage(recvPacket());
        isFin = p.isFIN();
        this.receivedPackages.add(p);
        this.socket.send(acknowledgePacket(p));
      } catch (ChecksumException e) {
        continue;
      }
    }
  }
  
  // Z3
  private void waitForRemainingPackages() throws IOException {
    int synseq = this.receivedPackages.stream()
        .mapToInt(Package::getSequenceNumber).min().getAsInt();
    int finseq = this.receivedPackages.stream()
        .mapToInt(Package::getSequenceNumber).max().getAsInt();
    int finalSize = (finseq - synseq);
    this.receivedPackages.removeIf(p -> p.getSequenceNumber() == synseq || p.getSequenceNumber() == finseq);
    while (currentSize() < finalSize) {
      try {
        Package p = extractPackage(recvPacket());
        this.receivedPackages.add(p);
        this.socket.send(acknowledgePacket(p));
      } catch (ChecksumException e) {
        continue;
      }
    }
  }
  
  private void writeFile() {
    try (FileOutputStream fos = new FileOutputStream(file)) {
      List<byte[]> payloads = receivedPackages.stream()
          .sorted((p1, p2) -> p1.getSequenceNumber() - p2.getSequenceNumber())
          .map(Package::getPayload)
          .collect(Collectors.toList());
      for (byte[] payload : payloads) {
        fos.write(payload);
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
  
  private int currentSize() {
    return receivedPackages.stream().mapToInt(p -> p.getPayload().length).sum();
  }
  
}
