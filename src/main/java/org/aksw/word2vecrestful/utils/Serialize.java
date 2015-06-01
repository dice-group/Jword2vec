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
    public synchronized static Object fromString(String s) throws IOException, ClassNotFoundException {
        byte[] data = Base64Coder.decode(s);
        ByteArrayInputStream bai = new ByteArrayInputStream(data);
        ObjectInputStream ois = new ObjectInputStream(bai);
        Object o = ois.readObject();
        ois.close();
        bai.close();
        return o;
    }

    /** Read the object from Base64 string. */
    public synchronized static Object fromByte(byte[] data) throws IOException, ClassNotFoundException {
        ByteArrayInputStream bai = new ByteArrayInputStream(data);
        ObjectInputStream ois = new ObjectInputStream(bai);
        Object o = ois.readObject();
        ois.close();
        bai.close();
        return o;
    }

    /** Write the object to a Base64 string. */
    public synchronized static String toString(Serializable o) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(o);
        oos.close();
        baos.close();
        return new String(Base64Coder.encode(baos.toByteArray()));
    }

    /** Write the object to a byte array. */
    public synchronized static byte[] toByte(Serializable o) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(o);
        oos.close();
        baos.close();
        return baos.toByteArray();
    }
}
