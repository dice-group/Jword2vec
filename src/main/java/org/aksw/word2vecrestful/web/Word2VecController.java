package org.aksw.word2vecrestful.web;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.sql.SQLException;
import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletResponse;

import org.aksw.word2vecrestful.db.DatabaseExtended;
import org.aksw.word2vecrestful.utils.Cfg;
import org.aksw.word2vecrestful.utils.Serialize;
import org.aksw.word2vecrestful.utils.Word2VecMath;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.json.JSONObject;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * 
 * @author rspeck
 *
 */
@RestController
@RequestMapping("/word2vec")
public class Word2VecController {

    public static Logger       LOG        = LogManager.getLogger(Word2VecController.class);
    public static final String CFG_APIKEY = Word2VecController.class.getName().concat(".apikey");

    protected String           apikey     = null;
    protected DatabaseExtended db         = null;
    protected final int        N          = 10;

    /**
     * 
     */
    @PostConstruct
    public void init() {

        apikey = Cfg.get(CFG_APIKEY);
        synchronized (this) {
            db = new DatabaseExtended();
        }
        writeShutDownFile("close");
    }

    /**
     * 
     * @param a
     * @param b
     * @return
     * @throws IOException
     * @throws ClassNotFoundException
     * @throws SQLException
     */
    @RequestMapping(
            value = "/distance",
            headers = "Accept=application/json",
            produces = "application/json",
            method = RequestMethod.GET)
    public @ResponseBody String distance(
            @RequestParam(value = "a") String a,
            @RequestParam(value = "apikey") String apikey,
            HttpServletResponse response
            )
    {
        if (!apikey.equals(this.apikey)) {
            try {
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "Wrong API key.");
                response.flushBuffer();
            } catch (IOException e) {
                LOG.error(e.getLocalizedMessage(), e);
            }

            return new JSONObject().toString();
        } else {
            synchronized (this) {
                try {
                    Map<String, Double> map = this.db.getNBest(N, a);
                    JSONObject jo = new JSONObject();
                    for (Entry<String, Double> entry : map.entrySet()) {
                        jo.put(entry.getKey(), entry.getValue());
                    }

                    response.setStatus(HttpServletResponse.SC_OK);
                    return jo.toString();

                } catch (Exception e) {
                    try {
                        response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getLocalizedMessage());
                        response.flushBuffer();
                    } catch (IOException ee) {
                        LOG.error(ee.getLocalizedMessage(), e);
                    }
                    return new JSONObject().toString();
                }
            }
        }
    }

    /**
     * 
     * @param a
     * @param b
     * @return
     * @throws IOException
     * @throws ClassNotFoundException
     * @throws SQLException
     */
    @RequestMapping(
            value = "/similarity",
            headers = "Accept=application/json",
            produces = "application/json",
            method = RequestMethod.GET)
    public @ResponseBody String similarity(
            @RequestParam(value = "a") String a,
            @RequestParam(value = "b") String b,
            @RequestParam(value = "apikey") String apikey,
            HttpServletResponse response
            )
    {
        if (!apikey.equals(this.apikey)) {
            try {
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "Wrong API key.");
                response.flushBuffer();
            } catch (IOException e) {
                LOG.error(e.getLocalizedMessage(), e);
            }

            return new JSONObject().toString();
        } else {
            synchronized (this) {
                try {
                    byte[] ab = db.getVec(a);
                    byte[] bb = db.getVec(b);
                    float[] af = (ab == null) ? null : (float[]) Serialize.fromByte(ab);
                    float[] bf = (bb == null) ? null : (float[]) Serialize.fromByte(bb);

                    if (af != null && bf != null) {

                        double sim = Word2VecMath.cosineSimilarity(af, bf);
                        response.setStatus(HttpServletResponse.SC_OK);
                        return new JSONObject()
                                .put("cos", sim)
                                .put("a", new JSONObject()
                                        .put("word", a)
                                        .put("vec", af))
                                .put("b", new JSONObject()
                                        .put("word", b)
                                        .put("vec", bf))
                                .toString();
                    } else {
                        response.setStatus(HttpServletResponse.SC_OK);
                        return new JSONObject()
                                .put("a", new JSONObject()
                                        .put("word", af == null ? "" : a))
                                .put("b", new JSONObject()
                                        .put("word", bf == null ? "" : b))
                                .toString();
                    }
                } catch (Exception e) {
                    try {
                        response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getLocalizedMessage());
                        response.flushBuffer();
                    } catch (IOException ee) {
                        LOG.error(ee.getLocalizedMessage(), e);
                    }
                    return new JSONObject().toString();
                }
            }
        }
    }

    /**
     * 
     * @param a
     * @return
     */
    @RequestMapping(
            value = "/vector",
            headers = "Accept=application/json",
            produces = "application/json",
            method = RequestMethod.GET)
    public @ResponseBody String vector(
            @RequestParam(value = "a") String a,
            @RequestParam(value = "apikey") String apikey,
            HttpServletResponse response)
    {
        if (!apikey.equals(this.apikey)) {
            try {
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "Wrong API key.");
                response.flushBuffer();
            } catch (IOException e) {
                LOG.error(e.getLocalizedMessage(), e);
            }
            return new JSONObject().toString();
        } else {
            synchronized (this) {
                try {
                    byte[] ab = db.getVec(a);
                    float[] av = (ab == null) ? null : (float[]) Serialize.fromByte(ab);

                    if (av != null) {
                        response.setStatus(HttpServletResponse.SC_OK);
                        return new JSONObject()
                                .put("a", new JSONObject()
                                        .put("word", a)
                                        .put("vec", av))
                                .toString();
                    }
                    else {
                        response.setStatus(HttpServletResponse.SC_OK);
                        return new JSONObject()
                                .put("a", new JSONObject()
                                        .put("word", av == null ? "" : a))
                                .toString();
                    }
                } catch (Exception e) {
                    try {
                        response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getLocalizedMessage());
                        response.flushBuffer();
                    } catch (IOException ee) {
                        LOG.error(ee.getLocalizedMessage(), ee);
                    }
                    return new JSONObject().toString();
                }
            }
        }
    }

    /**
     * Gives the applications process id.
     * 
     * @return applications process id
     */
    public static synchronized String getProcessId() {

        final String jvmName = ManagementFactory.getRuntimeMXBean().getName();
        final int index = jvmName.indexOf('@');
        if (index < 1)
            return null;
        try {
            return Long.toString(Long.parseLong(jvmName.substring(0, index)));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Writes a system depended file to shut down the application with kill cmd
     * and process id.
     * 
     * @return true if the file was written
     */
    public static synchronized boolean writeShutDownFile(String fileName) {

        // get process Id
        String id = getProcessId();
        if (id == null)
            return false;

        String cmd = "";
        String fileExtension = "";

        cmd = "kill " + id + System.getProperty("line.separator") + "rm " + fileName + ".sh";
        fileExtension = "sh";
        LOG.info(fileName + "." + fileExtension);

        File file = new File(fileName + "." + fileExtension);
        try {
            BufferedWriter out = new BufferedWriter(new FileWriter(file));
            out.write(cmd);
            out.close();
        } catch (Exception e) {
            LOG.error(e.getMessage());
        }
        file.setExecutable(true, false);
        file.deleteOnExit();
        return true;
    }
}
