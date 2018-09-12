package org.aksw.word2vecrestful.word2vec;

import java.io.File;

import org.aksw.word2vecrestful.utils.Cfg;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

public class Word2VecFactory {
	public static Logger LOG = LogManager.getLogger(Word2VecFactory.class);
	public static final String CFG_KEY_MODEL = Word2VecFactory.class.getName().concat(".model");
	public static String model = (Cfg.get(CFG_KEY_MODEL));
	public static final String CFG_KEY_BIN = Word2VecModelLoader.class.getName().concat(".bin");
	public static boolean binModel = Boolean.parseBoolean(Cfg.get(CFG_KEY_BIN));

	private static String nrmlMdlFilePath = (Cfg.get("org.aksw.word2vecrestful.word2vec.normalizedmodel.model"));
	private static boolean nrmlMdlBinFlg = Boolean
			.parseBoolean(Cfg.get("org.aksw.word2vecrestful.word2vec.normalizedmodel.bin"));

	private static String nrmlBinMdlFilePath = (Cfg.get("org.aksw.word2vecrestful.word2vec.normalizedbinmodel.model"));
	private static boolean nrmlBinMdlBinFlg = Boolean
			.parseBoolean(Cfg.get("org.aksw.word2vecrestful.word2vec.normalizedbinmodel.bin"));

	public static Word2VecModel get() {
		return new Word2VecModelLoader().loadModel(new File(model), binModel);
	}

	public static W2VNrmlMemModelNonIndxd getNormalizedModel() {
		Word2VecModel w2vmodel = new Word2VecModelLoader().loadModel(new File(nrmlMdlFilePath), nrmlMdlBinFlg);
		return new W2VNrmlMemModelNonIndxd(w2vmodel.word2vec, w2vmodel.vectorSize);
	}

	public static W2VNrmlMemModelNonIndxd getNormalizedBinNonIndxdModel() {
		LOG.info("Loading model to memory");
		Word2VecModel w2vmodel = new Word2VecModelLoader().loadModel(new File(nrmlBinMdlFilePath), nrmlBinMdlBinFlg);
		LOG.info("Model loaded to memory");
		LOG.info("Initializing "+W2VNrmlMemModelNonIndxd.class + " instance");
		W2VNrmlMemModelNonIndxd nrmlMemModel = new W2VNrmlMemModelNonIndxd(w2vmodel.word2vec, w2vmodel.vectorSize);
		LOG.info("Initialization of "+W2VNrmlMemModelNonIndxd.class + " instance finished");
		return nrmlMemModel;
	}
	
	public static Word2VecModel getNormalBinModel() {
		return new Word2VecModelLoader().loadModel(new File(nrmlBinMdlFilePath), nrmlBinMdlBinFlg);
	}
}
