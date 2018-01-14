package de.hm.cs.netze1;

import java.io.File;
import java.io.IOException;
import java.net.DatagramPacket;
import java.util.Arrays;
import java.util.Random;

public class FaultyFileReceiver extends FileReceiver {

  private DatagramPacket dupe = null;
  
  private double errorRate;
  private double dropRate;
  private double dupeRate;
  
  public FaultyFileReceiver(int port, File file, double errorRate, double dropRate, double dupeRate)
      throws IOException {
    super(port, file);
    this.errorRate = errorRate;
    this.dropRate = dropRate;
    this.dupeRate = dupeRate;
  }

  @Override
  protected DatagramPacket recvPacket() throws IOException {
    DatagramPacket dp;
    if (dupe != null) {
      dp = dupe;
      dupe = null;
    } else {
      dp = super.recvPacket();
    }
    if (Math.random() <= errorRate) {
      byte[] data = dp.getData();
      dp.setData(bitError(data));
    }
    if (Math.random() <= dropRate) {
      dp = recvPacket();
    }
    if (Math.random() <= dupeRate) {
      dupe = dp;
    }
    return dp;
  }

  private byte[] bitError(final byte[] data) {
    Random rand = new Random();
    byte[] faultyData = Arrays.copyOf(data, data.length);
    int i = rand.nextInt(data.length);
    int j = (int) Math.pow(2, rand.nextInt(8)); // Zufälliges bit
    faultyData[i] = (byte) (faultyData[i] ^ j); // Zufälliges bit im byte flippen mittels XOR
    return faultyData;
  }
  
}
