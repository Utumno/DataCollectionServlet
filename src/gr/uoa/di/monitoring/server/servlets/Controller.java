package gr.uoa.di.monitoring.server.servlets;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("serial")
class Controller extends HttpServlet {

	static final String DATA_COLLECTION_JSP = "/WEB-INF/jsp/data_collection.jsp";
	static ServletContext sc;
	Logger log;
	// private
	// "/WEB-INF/app.properties" also works...
	private static final String PROPERTIES_PATH = "WEB-INF/app.properties";
	private Properties properties;

	@Override
	public void init() throws ServletException {
		super.init();
		if (sc == null) sc = getServletContext();
		log = LoggerFactory.getLogger(this.getClass());
		try {
			loadProperties();
		} catch (IOException e) {
			throw new RuntimeException("Can't load properties file", e);
		}
	}

	private void loadProperties() throws IOException {
		InputStream inputStream = sc.getResourceAsStream(PROPERTIES_PATH);
		if (inputStream == null)
			throw new RuntimeException("Can't locate properties file");
		properties = new Properties();
		// load the inputStream using the Properties
		properties.load(inputStream);
	}

	String property(final String key) {
		return properties.getProperty(key);
	}
}
