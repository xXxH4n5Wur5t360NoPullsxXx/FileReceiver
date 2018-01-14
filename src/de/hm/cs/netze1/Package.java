package de.hm.cs.netze1;


import java.io.IOException;
import java.util.zip.Adler32;

public class Package {
  private byte flags = 0;
  private byte[] payload = new byte[]{0};
  private int sequenceNumber = 0;
  private int acknowledgementNumber = 0;
  private Long checksum = null;
	
	/*
	 * Flags
	 * 0: SYN
	 * 1: ACK
	 * 2: FACK
	 * 3: FIN
	 */

  public Package(byte[] paket) throws ChecksumException {
    checksum = (((paket[0] | 0x0L) << 56) & 0xFF00000000000000L)
        | (((paket[1] | 0x0L) << 48) & 0xFF000000000000L)
        | (((paket[2] | 0x0L) << 40) & 0xFF0000000000L)
        | (((paket[3] | 0x0L) << 32) & 0xFF00000000L)
        | (((paket[4] | 0x0L) << 24) & 0xFF000000L)
        | (((paket[5] | 0x0L) << 16) & 0xFF0000L)
        | (((paket[6] | 0x0L) <<  8) & 0xFF00L)
        | ((paket[7] | 0x0L) & 0xFFL);
    Adler32 chinese = new Adler32();
    chinese.update(paket, 8, paket.length - 8);
    if (checksum.compareTo(chinese.getValue()) != 0) {
      throw new ChecksumException();
    }
    this.sequenceNumber = ((paket[8] << 24) & 0xFF000000)
        | ((paket[9] << 16) & 0xFF0000)
        | ((paket[10] << 8) & 0xFF00)
        | (paket[11] & 0xFF);
    this.acknowledgementNumber = ((paket[12] << 24) & 0xFF000000)
        | ((paket[13] << 16) & 0xFF0000)
        | ((paket[14] << 8) & 0xFF00)
        | (paket[15] & 0xFF);
    this.flags = paket[16];
    this.payload = new byte[paket.length - 17];
    System.arraycopy(paket, 17, this.payload, 0, this.payload.length);
  }

  public Package() {

  }

  public byte[] makePaket() {
    byte[] paket = new byte[payload.length + 17];
    paket[8] = (byte) (this.sequenceNumber >> 24);
    paket[9] = (byte) (this.sequenceNumber >> 16);
    paket[10] = (byte) (this.sequenceNumber >> 8);
    paket[11] = (byte) this.sequenceNumber;
    paket[12] = (byte) (this.acknowledgementNumber >> 24);
    paket[13] = (byte) (this.acknowledgementNumber >> 16);
    paket[14] = (byte) (this.acknowledgementNumber >> 8);
    paket[15] = (byte) this.acknowledgementNumber;
    paket[16] = this.flags;
    System.arraycopy(this.payload, 0, paket, 17, this.payload.length);
    Adler32 chinese = new Adler32();
    chinese.update(paket, 8,paket.length - 8);
    checksum = chinese.getValue();
    paket[0] = (byte) (checksum >> 56L);
    paket[1] = (byte) (checksum >> 48L);
    paket[2] = (byte) (checksum >> 40L);
    paket[3] = (byte) (checksum >> 32L);
    paket[4] = (byte) (checksum >> 24L);
    paket[5] = (byte) (checksum >> 16L);
    paket[6] = (byte) (checksum >> 8L);
    paket[7] = (byte) (checksum >> 0L);
    return paket;
  }

  public void setACK(boolean ack) {
    flags = ack ? (byte) (flags | 2) : (byte) (flags & 253);
  }

  public void setFACK(boolean fack) {
    flags = fack ? (byte) (flags | 4) : (byte) (flags & 251);
  }

  public void setSYN(boolean syn) {
    flags = syn ? (byte) (flags | 1) : (byte) (flags & 254);
  }

  public void setFIN(boolean fin) {
    flags = fin ? (byte) (flags | 8) : (byte) (flags & 247);
  }

  public boolean isSYN() {
    return (flags & 1) != 0;
  }

  public boolean isACK() {
    return (flags & 2) != 0;
  }

  public boolean isFACK() {
    return (flags & 4) != 0;
  }

  public boolean isFIN() {
    return (flags & 8) != 0;
  }

  public void setPayload(byte[] payload, int length) {
    this.payload = new byte[payload.length];
    System.arraycopy(payload, 0, this.payload, 0, length);
  }

  public byte[] getPayload() {
    return this.payload;
  }

  public int getSequenceNumber() {
    return sequenceNumber;
  }

  public void setSequenceNumber(int sequenceNumber) {
    this.sequenceNumber = sequenceNumber;
  }

  public int getAcknowledgementNumber() {
    return acknowledgementNumber;
  }

  public void setAcknowledgementNumber(int acknowledgementNumber) {
    this.acknowledgementNumber = acknowledgementNumber;
  }

  private Long getChecksum() {
    byte[] paket = new byte[payload.length + 9];
    paket[0] = (byte) (this.sequenceNumber >> 24);
    paket[1] = (byte) (this.sequenceNumber >> 16);
    paket[2] = (byte) (this.sequenceNumber >> 8);
    paket[3] = (byte) this.sequenceNumber;
    paket[4] = (byte) (this.acknowledgementNumber >> 24);
    paket[5] = (byte) (this.acknowledgementNumber >> 16);
    paket[6] = (byte) (this.acknowledgementNumber >> 8);
    paket[7] = (byte) this.acknowledgementNumber;
    paket[8] = this.flags;
    System.arraycopy(this.payload, 0, paket, 9, this.payload.length);
    Adler32 chinese = new Adler32();
    chinese.update(paket, 0, paket.length);
    return chinese.getValue();
  }

  @Override
  public int hashCode() {
    return (int) (getChecksum() << 0L);
  }

  @Override
  public boolean equals(Object other) {
    if (other == null) {
      return false;
    }
    if (other.getClass() != getClass()) {
      return false;
    }

    return ((Package)other).getChecksum().compareTo(getChecksum()) == 0;
  }
}
