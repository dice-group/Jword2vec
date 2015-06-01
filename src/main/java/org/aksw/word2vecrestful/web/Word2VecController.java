package org.aksw.word2vecrestful.web;

import java.io.IOException;
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
    private final int          MAX        = 100;

    /**
     * 
     */
    @PostConstruct
    public void init() {
        apikey = Cfg.get(CFG_APIKEY);
        db = new DatabaseExtended();
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
            @RequestParam(value = "n") Integer n,
            @RequestParam(value = "apikey") String apikey,
            HttpServletResponse response
            )
    {
        // check parameters
        if (!apikey.equals(this.apikey)) {
            try {
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "Wrong API key.");
                response.flushBuffer();
            } catch (IOException e) {
                LOG.error(e.getLocalizedMessage(), e);
            }

            return new JSONObject().toString();
        }
        if (n < 1) {
            try {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "N has to be greater than 0.");
                response.flushBuffer();
            } catch (IOException e) {
                LOG.error(e.getLocalizedMessage(), e);
            }
            return new JSONObject().toString();
        }

        if (a.trim().isEmpty()) {
            try {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Please send a word.");
                response.flushBuffer();
            } catch (IOException e) {
                LOG.error(e.getLocalizedMessage(), e);
            }
            return new JSONObject().toString();
        }
        // ----

        // max n
        if (n > MAX) {
            n = MAX;
        }

        synchronized (db) {
            try {
                Map<String, Double> map = db.getNBest(n, a);
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
            synchronized (db) {
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
            synchronized (db) {
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
}
