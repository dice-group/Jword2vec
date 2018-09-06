package org.aksw.word2vecrestful.word2vec;

import java.io.File;

import org.aksw.word2vecrestful.utils.Cfg;

public class Word2VecFactory {

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

	public static W2VNrmlMemModel getNormalizedModel() {
		Word2VecModel w2vmodel = new Word2VecModelLoader().loadModel(new File(nrmlMdlFilePath), nrmlMdlBinFlg);
		return new W2VNrmlMemModel(w2vmodel.word2vec, w2vmodel.vectorSize);
	}

	public static W2VNrmlMemModel getNormalizedBinModel() {
		Word2VecModel w2vmodel = new Word2VecModelLoader().loadModel(new File(nrmlBinMdlFilePath), nrmlBinMdlBinFlg);
		return new W2VNrmlMemModel(w2vmodel.word2vec, w2vmodel.vectorSize);
	}
}
