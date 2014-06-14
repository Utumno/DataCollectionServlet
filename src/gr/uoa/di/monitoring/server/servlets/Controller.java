package gr.uoa.di.monitoring.server.servlets;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
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
	static File workDir;
	Logger log;
	// private
	// "/WEB-INF/app.properties" also works...
	private static final String PROPERTIES_PATH = "/WEB-INF/app.properties";
	private static final String TEMP_DIR = Controller.class.getPackage()
			.toString().split(" ")[1];
	private Properties properties;

	@Override
	public void init() throws ServletException {
		super.init();
		if (sc == null) sc = getServletContext();
		log = LoggerFactory.getLogger(this.getClass());
		log.info("CWD : " + new File(".").getAbsolutePath());
		final Object tmpDirAttr = sc.getAttribute(ServletContext.TEMPDIR);
		log.info("Tmp Dir : " + tmpDirAttr);
		try {
			loadProperties();
		} catch (IOException e) {
			throw new RuntimeException("Can't load properties file", e);
		}
		// final Enumeration<String> attributeNames = sc.getAttributeNames();
		// for (; attributeNames.hasMoreElements();) {
		// log.debug(attributeNames.nextElement());
		// }
		// see : http://www.znetdevelopment.com/blogs/2012/03/14/
		// File tmpDir = (File)
		// sc.getAttribute("javax.servlet.context.tempdir");
		File tmpDir = (File) tmpDirAttr;
		if (tmpDir == null) {
			throw new ServletException(
					"Servlet container does not provide temporary directory");
		}
		workDir = new File(tmpDir, TEMP_DIR);
		log.info("workDir : " + workDir.getAbsolutePath());
		if (!(workDir.exists() && workDir.isDirectory()) && !workDir.mkdirs()) {
			throw new ServletException("Unable to create "
				+ workDir.getAbsolutePath() + " temporary directory");
		}
	}

	@Override
	public void destroy() {
		final String workDirPath = workDir.getAbsolutePath();
		try {
			removeRecursive(Paths.get(workDirPath));
			log.debug("Deleted tmp directory " + workDirPath);
		} catch (IOException e) {
			log.warn("Failed to delete tmp directory in " + workDirPath, e);
		}
		super.destroy();
	}

	private void loadProperties() throws IOException {
		try (InputStream inputStream = sc.getResourceAsStream(PROPERTIES_PATH)) {
			if (inputStream == null)
				throw new RuntimeException("Can't locate properties file");
			properties = new Properties();
			// load the inputStream using the Properties
			properties.load(inputStream);
		}
	}

	String property(final String key) {
		return properties.getProperty(key);
	}

	// http://stackoverflow.com/a/8685959/281545
	// should be in a helpers class
	static void removeRecursive(Path path) throws IOException {
		Files.walkFileTree(path, new SimpleFileVisitor<Path>() {

			final Logger logger = LoggerFactory.getLogger(this.getClass());
			@Override
			public FileVisitResult visitFile(Path file,
					BasicFileAttributes attrs) throws IOException {
				logger.debug("Deleting " + file.getFileName());
				Files.delete(file);
				logger.debug("DELETED " + file.getFileName());
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult visitFileFailed(Path file, IOException exc) {
				// try to delete the file anyway, even if its attributes
				// could not be read, since delete-only access is
				// theoretically possible
				logger.warn(
					"Delete file " + file + " failed", exc);
				try {
					Files.delete(file);
				} catch (IOException e) {
					logger.warn(
						"Delete file " + file + " failed again", exc);
				}
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult postVisitDirectory(Path dir, IOException exc)
					throws IOException {
				if (exc == null) {
					Files.delete(dir);
					return FileVisitResult.CONTINUE;
				}
				// directory iteration failed; propagate exception
				throw exc;
			}
		});
	}
}
