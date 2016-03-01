package org.aksw.word2vecrestful.web;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletResponse;

import org.aksw.word2vecrestful.Application;
import org.aksw.word2vecrestful.db.IDatabase;
import org.aksw.word2vecrestful.db.InMemoryDB;
import org.aksw.word2vecrestful.db.SQLightDB;
import org.aksw.word2vecrestful.utils.Cfg;
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

  public static Logger LOG = LogManager.getLogger(Word2VecController.class);

  public static final String CFG_APIKEY = Word2VecController.class.getName().concat(".apikey");
  public static final String CFG_API_MAX_N = Word2VecController.class.getName().concat(".maxN");

  protected final String apikey = Cfg.get(CFG_APIKEY);
  private final int MAX = Integer.parseInt(Cfg.get(CFG_API_MAX_N));

  protected IDatabase db = null;

  /**
   * Initializes the database (in memory or sqlight).
   */
  @PostConstruct
  public void init() {
    if (Application.inmem) {
      db = new InMemoryDB();
    } else {
      db = new SQLightDB();
    }
  }

  /**
  *
  */
  @RequestMapping(value = "/operation", headers = "Accept=application/json",
      produces = "application/json", method = RequestMethod.GET)
  public @ResponseBody String operation(@RequestParam(value = "words") final String words,
      @RequestParam(value = "n") Integer n, @RequestParam(value = "apikey") final String apikey,
      final HttpServletResponse response) {
    if (!apikey.equals(this.apikey)) {
      try {
        response.sendError(HttpServletResponse.SC_FORBIDDEN, "Wrong API key.");
        response.flushBuffer();
      } catch (final IOException e) {
        LOG.error(e.getLocalizedMessage(), e);
      }

      return new JSONObject().toString();
    }
    if ((words == null) || words.trim().isEmpty()) {
      try {
        response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Please send a word.");
        response.flushBuffer();
      } catch (final IOException e) {
        LOG.error(e.getLocalizedMessage(), e);
      }
      return new JSONObject().toString();
    }
    // --
    // parse the input
    final char[] cw = words.toCharArray();

    final List<String> aopers = new ArrayList<>();
    final List<float[]> vecs = new ArrayList<>();

    boolean done = false;
    StringBuffer currentWord = new StringBuffer();
    for (int i = 0; i < cw.length; i++) {
      if (cw[i] == '+') {
        aopers.add("+");
        done = true;
      } else if (cw[i] == '-') {
        aopers.add("-");
        done = true;
      } else {
        currentWord.append(cw[i]);
      }
      if (done || ((i + 1) == cw.length)) {
        final String m = currentWord.toString();

        final float[] addvec = db.getVec(m);
        LOG.info(m + " " + addvec.length);

        vecs.add(addvec);
        done = false;
        currentWord = new StringBuffer();
      }
    }

    // check if the amount of operators matches the vecs

    if ((vecs.size() > 0) && ((aopers.size() + 1) == vecs.size())) {
      String currentOperator = "";
      float[] currentVec = vecs.get(0);
      for (int i = 0; i < aopers.size(); i++) {
        final float[] vectodo = vecs.get(i + 1);
        if ((vectodo != null) && (currentVec != null)) {
          currentOperator = aopers.get(i);

          if (currentOperator.equals("+")) {
            currentVec = Word2VecMath.add(currentVec, vectodo);

          } else if (currentOperator.equals("-")) {
            currentVec = Word2VecMath.sub(currentVec, vectodo);
          } else {
            LOG.warn("Something went wrong...");
          }
        } else {
          LOG.error("There is a null vector");
        }
      }

      // TODO: find closes word to the finel vec

      // max n
      if (n > MAX) {
        n = MAX;
      }
      final JSONObject jo = new JSONObject();
      synchronized (db) {
        try {
          final Map<String, Double> map = db.getNClosest(currentVec, n);
          // map to json

          for (final Entry<String, Double> entry : map.entrySet()) {
            jo.put(entry.getKey(), entry.getValue());
          }
          response.setStatus(HttpServletResponse.SC_OK);
          return jo.toString();
        } catch (final Exception e) {
          try {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                e.getLocalizedMessage());
            response.flushBuffer();
          } catch (final IOException ee) {
            LOG.error(ee.getLocalizedMessage(), e);
          }
          return new JSONObject().toString();
        }
      }

    } else {
      try {
        response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Input is wrong.");
        response.flushBuffer();
      } catch (final IOException e) {
        LOG.error(e.getLocalizedMessage(), e);
      }
      return new JSONObject().toString();
    }

  }

  /**
   *
   */
  @RequestMapping(value = "/distance", headers = "Accept=application/json",
      produces = "application/json", method = RequestMethod.GET)
  public @ResponseBody String distance(@RequestParam(value = "a") final String a,
      @RequestParam(value = "n") Integer n, @RequestParam(value = "apikey") final String apikey,
      final HttpServletResponse response) {
    // check parameters
    if (!apikey.equals(this.apikey)) {
      try {
        response.sendError(HttpServletResponse.SC_FORBIDDEN, "Wrong API key.");
        response.flushBuffer();
      } catch (final IOException e) {
        LOG.error(e.getLocalizedMessage(), e);
      }

      return new JSONObject().toString();
    }
    if (n < 1) {
      try {
        response.sendError(HttpServletResponse.SC_BAD_REQUEST, "N has to be greater than 0.");
        response.flushBuffer();
      } catch (final IOException e) {
        LOG.error(e.getLocalizedMessage(), e);
      }
      return new JSONObject().toString();
    }

    if (a.trim().isEmpty()) {
      try {
        response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Please send a word.");
        response.flushBuffer();
      } catch (final IOException e) {
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
        final Map<String, Double> map = db.getNBest(a, n);
        // map to json
        final JSONObject jo = new JSONObject();
        for (final Entry<String, Double> entry : map.entrySet()) {
          jo.put(entry.getKey(), entry.getValue());
        }
        response.setStatus(HttpServletResponse.SC_OK);
        return jo.toString();
      } catch (final Exception e) {
        try {
          response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getLocalizedMessage());
          response.flushBuffer();
        } catch (final IOException ee) {
          LOG.error(ee.getLocalizedMessage(), e);
        }
        return new JSONObject().toString();
      }
    }
  }

  /**
   *
   */
  @RequestMapping(value = "/similarity", headers = "Accept=application/json",
      produces = "application/json", method = RequestMethod.GET)
  public @ResponseBody String similarity(@RequestParam(value = "a") final String a,
      @RequestParam(value = "b") final String b,
      @RequestParam(value = "apikey") final String apikey, final HttpServletResponse response) {
    if (!apikey.equals(this.apikey)) {
      try {
        response.sendError(HttpServletResponse.SC_FORBIDDEN, "Wrong API key.");
        response.flushBuffer();
      } catch (final IOException e) {
        LOG.error(e.getLocalizedMessage(), e);
      }

      return new JSONObject().toString();
    } else {
      synchronized (db) {
        try {
          final float[] af = db.getVec(a);
          final float[] bf = db.getVec(b);

          if ((af != null) && (bf != null)) {
            final double sim = Word2VecMath.cosineSimilarity(af, bf);

            response.setStatus(HttpServletResponse.SC_OK);
            return new JSONObject().put("cos", sim)
                .put("a", new JSONObject().put("word", a).put("vec", af))
                .put("b", new JSONObject().put("word", b).put("vec", bf)).toString();
          } else {
            response.setStatus(HttpServletResponse.SC_OK);
            return new JSONObject().put("a", new JSONObject().put("word", af == null ? "" : a))
                .put("b", new JSONObject().put("word", bf == null ? "" : b)).toString();
          }
        } catch (final Exception e) {
          try {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                e.getLocalizedMessage());
            response.flushBuffer();
          } catch (final IOException ee) {
            LOG.error(ee.getLocalizedMessage(), e);
          }
          return new JSONObject().toString();
        }
      }
    }
  }

  /**
   *
   */
  @RequestMapping(value = "/vector", headers = "Accept=application/json",
      produces = "application/json", method = RequestMethod.GET)
  public @ResponseBody String vector(@RequestParam(value = "a") final String a,
      @RequestParam(value = "apikey") final String apikey, final HttpServletResponse response) {
    if (!apikey.equals(this.apikey)) {
      try {
        response.sendError(HttpServletResponse.SC_FORBIDDEN, "Wrong API key.");
        response.flushBuffer();
      } catch (final IOException e) {
        LOG.error(e.getLocalizedMessage(), e);
      }
      return new JSONObject().toString();

    } else {

      synchronized (db) {
        try {
          final float[] av = db.getVec(a);

          if (av != null) {
            response.setStatus(HttpServletResponse.SC_OK);
            return new JSONObject().put("a", new JSONObject().put("word", a).put("vec", av))
                .toString();
          } else {
            response.setStatus(HttpServletResponse.SC_OK);
            return new JSONObject().put("a", new JSONObject().put("word", av == null ? "" : a))
                .toString();
          }
        } catch (final Exception e) {
          try {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                e.getLocalizedMessage());
            response.flushBuffer();
          } catch (final IOException ee) {
            LOG.error(ee.getLocalizedMessage(), ee);
          }
          return new JSONObject().toString();
        }
      }
    }
  }
}
