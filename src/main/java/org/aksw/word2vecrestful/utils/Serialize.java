package org.aksw.word2vecrestful.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import org.yaml.snakeyaml.external.biz.base64Coder.Base64Coder;

public class Serialize {

  /** Read the object from Base64 string. */
  public synchronized static Object fromString(final String s)
      throws IOException, ClassNotFoundException {
    final byte[] data = Base64Coder.decode(s);
    final ByteArrayInputStream bai = new ByteArrayInputStream(data);
    final ObjectInputStream ois = new ObjectInputStream(bai);
    final Object o = ois.readObject();
    ois.close();
    bai.close();
    return o;
  }

  /** Read the object from Base64 string. */
  public synchronized static Object fromByte(final byte[] data)
      throws IOException, ClassNotFoundException {
    final ByteArrayInputStream bai = new ByteArrayInputStream(data);
    final ObjectInputStream ois = new ObjectInputStream(bai);
    final Object o = ois.readObject();
    ois.close();
    bai.close();
    return o;
  }

  /** Write the object to a Base64 string. */
  public synchronized static String toString(final Serializable o) throws IOException {
    final ByteArrayOutputStream baos = new ByteArrayOutputStream();
    final ObjectOutputStream oos = new ObjectOutputStream(baos);
    oos.writeObject(o);
    oos.close();
    baos.close();
    return new String(Base64Coder.encode(baos.toByteArray()));
  }

  /** Write the object to a byte array. */
  public synchronized static byte[] toByte(final Serializable o) throws IOException {
    final ByteArrayOutputStream baos = new ByteArrayOutputStream();
    final ObjectOutputStream oos = new ObjectOutputStream(baos);
    oos.writeObject(o);
    oos.close();
    baos.close();
    return baos.toByteArray();
  }
}
