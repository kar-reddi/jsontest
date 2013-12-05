package com.goeuro;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonValue;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLSocketFactory;

/**
 * This class uses the Json Object Model API mechanism. SSL certificate needs to
 * be loaded in to key store, in case of not loaded, there is fall back
 * mechanism to ignore certification validation. In real scenario we should not
 * ignore certification validation but for this Test purpose we use this.
 * 
 * @author sreddi2
 * 
 */
public class JsonObjectModelParsing {

	/**
	 * This main method needs an argument of to pass Json end point
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		if (0 == args.length || "".equals(args[0].trim())) {
			System.out.println("Please pass the STRING as an argument");
			return;
		}
		JsonObjectModelParsing test = new JsonObjectModelParsing();
		try {
			final String result = test.useObjectModel(args[0]);
			if (result != null) {

				GoEuroUtil.saveToFile(result, args[0]);
				System.out.println("Sucessfully " + args[0]
						+ ".csv file has created with results");
			}
		} catch (IOException ie) {
			System.out.println("Technical Problem. This shouldn't happen");
			throw new IllegalStateException(ie.getLocalizedMessage());
		}

	}

	/**
	 * This method first attempt to connect using SSL connection. It will
	 * fail,If the certificate is not stored in local keystore. To avoid such an
	 * error, I use dummy keystore to avoid certificate validation.
	 * 
	 * @param location
	 * @return
	 * @throws IOException
	 */
	private String useObjectModel(final String location) throws IOException {

		boolean firstRow = true;
		final StringBuilder resultString = new StringBuilder();
		InputStream is = null;
		try {
			final URL url = new URL(GoEuroUtil.END_POINT_URL + location);
			final URLConnection urlCon = url.openConnection();

			try {
				// First try the sslhandshake by assuming that ssl certificate
				// is imported into truststore by using keytool
				is = urlCon.getInputStream();
			} catch (SSLHandshakeException se) {
				// Tell the url connection object to use our socket factory
				// which bypasses security checks
				URLConnection urc = url.openConnection();
				SSLSocketFactory sslSocketFactory = GoEuroUtil
						.getSSLSocketFactory();
				((HttpsURLConnection) urc)
						.setSSLSocketFactory(sslSocketFactory);
				is = urc.getInputStream();
			}
			if (is == null) {
				System.out.println("Cann't connect to Server.Please check it.");
				return null;
			}
			JsonReader rdr = Json.createReader(is);
			JsonObject obj = rdr.readObject();
			JsonArray results = obj.getJsonArray("results");
			if (0 == results.size()) {
				System.out.println("No Results Found with given parameter:"
						+ location);
				return null;
			}

			for (JsonObject result : results.getValuesAs(JsonObject.class)) {
				if (firstRow) {
					final String temp = GoEuroUtil
							.removeEndComma(prepareHeader(result));
					resultString.append(temp + GoEuroUtil.NEW_LINE);
					firstRow = false;
				}
				final String temp = GoEuroUtil
						.removeEndComma(prepareRow(result)
								+ GoEuroUtil.NEW_LINE);
				resultString.append(temp);

			}

		} finally {
			if (null != is) {
				is.close();
			}
		}
		return resultString.toString();
	}

	/*
	 * Prepare a row value for .csv file
	 */
	private String prepareRow(final JsonObject row) {
		final StringBuilder builder = new StringBuilder();
		for (JsonValue value : row.values()) {
			switch (value.getValueType()) {
			case OBJECT:
				final String tempValue = prepareRow((JsonObject) value);
				builder.append(tempValue);
				break;
			case STRING:
			case NUMBER:
			case TRUE:
			case FALSE:
				builder.append(value.toString() + GoEuroUtil.SEPERATER);
				break;
			default:

			}
		}
		return builder.toString();
	}

	/**
	 * prepares a header for .csv file
	 * 
	 * @param row
	 * @return
	 */
	private String prepareHeader(final JsonObject row) {
		final StringBuilder builder = new StringBuilder();
		for (String key : row.keySet()) {
			JsonValue jValue = row.get(key);
			switch (jValue.getValueType()) {
			case OBJECT:
				JsonObject obj = (JsonObject) jValue;
				builder.append(prepareHeader(obj));
				break;
			case STRING:
			case NUMBER:
			case TRUE:
			case FALSE:
				builder.append(key + GoEuroUtil.SEPERATER);
				break;
			default:
			}
		}
		return builder.toString();
	}

}
