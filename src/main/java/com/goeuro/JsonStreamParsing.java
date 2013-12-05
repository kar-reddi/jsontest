package com.goeuro;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

import javax.json.Json;
import javax.json.stream.JsonParser;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLSocketFactory;

/**
 * This class uses the Json Stream parsing mechanism to achieve better
 * performance and low memory usage. SSL certificate needs to be loaded in to
 * key store, in case of not loaded, there is fall back mechanism to ignore
 * certification validation. In real scenario we should not ignore certification
 * validation but for this Test purpose we use this.
 * 
 * @author sreddi2
 * 
 */
public class JsonStreamParsing {

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
		JsonStreamParsing test = new JsonStreamParsing();
		try {
			final String result = test.useStreamParsing(args[0]);
			if (result != null) {

				GoEuroUtil.saveToFile(result, args[0]);
				System.out.println("Sucessfully " + args[0]
						+ ".csv file has created with results");
			} else {
				System.out.println("No Results Found");
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
	public String useStreamParsing(final String location) throws IOException {
		final StringBuilder sBuilder = new StringBuilder();
		final StringBuilder header = new StringBuilder();
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
			JsonParser parser = Json.createParser(is);
			boolean startArray = false;
			boolean firstRow = false;
			boolean keySNote = false;
			String keyName = "";

			StringBuilder row = new StringBuilder();
			while (parser.hasNext()) {

				JsonParser.Event event = parser.next();
				switch (event) {
				case START_ARRAY:
					startArray = true;
					firstRow = true;
					break;
				case END_ARRAY:
				case START_OBJECT:
					if (startArray && firstRow) {
						keySNote = true;
					}
					break;
				case END_OBJECT:
					if (row.length() > 0) {
						final String rowV = GoEuroUtil.removeEndComma(row
								.toString());
						sBuilder.append(rowV + GoEuroUtil.NEW_LINE);
						firstRow = false;
						row.delete(0, row.length());
					}
					break;
				case VALUE_FALSE:
				case VALUE_NULL:
				case VALUE_TRUE:
				case VALUE_STRING:
				case VALUE_NUMBER:
					if (!"".equals(keyName)) {
						if (firstRow) {
							header.append(keyName + GoEuroUtil.SEPERATER);
						}
						row.append(parser.getString() + GoEuroUtil.SEPERATER);
						keyName = "";
					}

					break;
				case KEY_NAME:
					if (keySNote) {
						keyName = parser.getString();
					}
					break;
				}
			}

		} finally {
			if (null != is)
				is.close();
		}
		if (header.length() == 0) {
			return null;
		}
		final String headerValue = GoEuroUtil.removeEndComma(header.toString())
				+ GoEuroUtil.NEW_LINE;
		return headerValue + sBuilder.toString();
	}

}
