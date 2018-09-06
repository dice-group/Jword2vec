package org.aksw.word2vecrestful.db.mongo;

import org.bson.Document;

import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

public class MongoDbHandler {
	private String dbName;
	private String host = "localhost";
	private Integer port = 27017;
	private MongoDatabase database;
	private MongoClient mongoClient;

	public MongoDbHandler(String dbName, String host, Integer port) {
		if (host != null) {
			this.host = host;
		}
		if (port != null) {
			this.port = port;
		}
		this.dbName = dbName;
	}

	public void connect() {
		this.mongoClient = new MongoClient(this.host, this.port);
		this.database = mongoClient.getDatabase(this.dbName);
	}

	public void close() {
		this.mongoClient.close();
	}

	public MongoCollection<Document> createCollection(String collctnName) {
		this.database.createCollection(collctnName);
		return this.database.getCollection(collctnName);
	}

	// Setter and Getters

	public String getDbName() {
		return dbName;
	}

	public void setDbName(String dbName) {
		this.dbName = dbName;
	}

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public Integer getPort() {
		return port;
	}

	public void setPort(Integer port) {
		this.port = port;
	}

	public MongoDatabase getDatabase() {
		return database;
	}

	public void setDatabase(MongoDatabase database) {
		this.database = database;
	}

	public MongoClient getMongoClient() {
		return mongoClient;
	}

	public void setMongoClient(MongoClient mongoClient) {
		this.mongoClient = mongoClient;
	}

}
