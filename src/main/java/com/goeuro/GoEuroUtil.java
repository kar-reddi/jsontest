package com.goeuro;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class GoEuroUtil {
	public final static String SEPERATER = ",";
	public final static String NEW_LINE = "\n";
	public final static String FILE_EXTN = ".csv";
	public final static String END_POINT_URL = "https://api.goeuro.de/api/v1/suggest/position/en/name/";

	/**
	 * Helper to method to save the result to a .csv file
	 * 
	 * @param content
	 * @param fileName
	 * @throws IOException
	 */
	protected static void saveToFile(final String content, final String fileName)
			throws IOException {
		final Writer fileWriter = new OutputStreamWriter(new FileOutputStream(
				new File(fileName + FILE_EXTN)));
		try {

			fileWriter.write(content);
		} catch (FileNotFoundException fe) {
			System.out.println("Technical Problem. This shouldn't happen");
			throw new IllegalStateException(fe.getLocalizedMessage());
		} finally {
			fileWriter.close();
		}
	}

	/**
	 * Removes the "," if exists at end of a row
	 * 
	 * @param value
	 * @return
	 */
	protected static String removeEndComma(final String value) {
		if (value.endsWith(SEPERATER)) {
			return value.substring(0, value.length() - 1);
		} else {
			return value;
		}
	}

	/**
	 * implements dummy keystore and getSSLScoketFactory using dummy Kerystore.
	 * 
	 * @return
	 */
	protected static SSLSocketFactory getSSLSocketFactory() {
		SSLSocketFactory sslSocketFactory = null;

		// Create a trust manager that does not validate certificate chains
		final TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {

			@Override
			public X509Certificate[] getAcceptedIssuers() {
				return null;
			}

			@Override
			public void checkServerTrusted(X509Certificate[] chain,
					String authType) throws CertificateException {
			}

			@Override
			public void checkClientTrusted(X509Certificate[] chain,
					String authType) throws CertificateException {

			}
		} };
		try {
			// Install the all-trusting trust manager
			final SSLContext sslContext = SSLContext.getInstance("SSL");
			sslContext.init(null, trustAllCerts,
					new java.security.SecureRandom());
			// Create an ssl socket factory with our all-trusting manager
			sslSocketFactory = sslContext.getSocketFactory();

		} catch (NoSuchAlgorithmException e) {
			System.out.println("Technical Problem. This shouldn't happen");
			throw new IllegalStateException(e.getLocalizedMessage());
		} catch (KeyManagementException ke) {
			System.out.println("Technical Problem. This shouldn't happen");
			throw new IllegalStateException(ke.getLocalizedMessage());
		}
		return sslSocketFactory;
	}

}
