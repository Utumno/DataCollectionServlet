package gr.uoa.di.monitoring.server.servlets;

import gr.uoa.di.android.helpers.Zip;
import gr.uoa.di.android.helpers.Zip.CompressException;
import gr.uoa.di.monitoring.android.persist.FileStore;
import gr.uoa.di.monitoring.model.Data;
import gr.uoa.di.monitoring.model.ParserException;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;

@SuppressWarnings("serial")
@WebServlet("/")
@MultipartConfig
public final class DataCollectionServlet extends Controller {

	// TODO create the dir in the server on init
	private static final String UPLOAD_LOCATION_PROPERTY_KEY = "upload.location";
	private String uploadsDirName;

	@Override
	public void init() throws ServletException {
		super.init();
		uploadsDirName = property(UPLOAD_LOCATION_PROPERTY_KEY);
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		Map<String, Map<Class<? extends Data>, List<Data>>> master_map = new HashMap<>();
		for (File node : new File(uploadsDirName).listFiles()) {
			if (node.isDirectory()) {
				// directories contain the (continually updated) files for each
				// device
				try {
					final String dirname = node.getName(); // device id
					master_map.put(dirname,
						FileStore.parse(node.getAbsolutePath(), dirname));
				} catch (ParserException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		req.setAttribute("master_map", master_map);
		sc.getRequestDispatcher(DATA_COLLECTION_JSP).forward(req, resp);
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		Collection<Part> parts = req.getParts();
		/*
		 * FIXME : java.io.IOException:
		 * org.apache.tomcat.util.http.fileupload.FileUploadException: Read
		 * timed out at
		 * org.apache.catalina.connector.Request.parseParts(Request.java:2786)
		 * at org.apache.catalina.connector.Request.getParts(Request.java:2640)
		 * at
		 * org.apache.catalina.connector.RequestFacade.getParts(RequestFacade.
		 * java:1076) at
		 * gr.uoa.di.monitoring.server.servlets.DataCollectionServlet
		 * .doPost(DataCollectionServlet.java:64) at
		 * javax.servlet.http.HttpServlet.service(HttpServlet.java:641) ...
		 * Caused by:
		 * org.apache.tomcat.util.http.fileupload.FileUploadException: Read
		 * timed out at
		 * org.apache.tomcat.util.http.fileupload.FileUploadBase.parseRequest
		 * (FileUploadBase.java:338) at
		 * org.apache.tomcat.util.http.fileupload.servlet
		 * .ServletFileUpload.parseRequest(ServletFileUpload.java:129) at
		 * org.apache.catalina.connector.Request.parseParts(Request.java:2722)
		 * ... 21 more Caused by: java.net.SocketTimeoutException: Read timed
		 * out at java.net.SocketInputStream.socketRead0(Native Method)...
		 */
		for (Part part : parts) {
			// save the zip into uploads dir
			final String filename = getFilename(part);
			File save = new File(uploadsDirName, filename + "_"
				+ System.currentTimeMillis() + ".zip");
			final String imei = FileStore.getDeviceID(filename);
			final String absolutePath = save.getAbsolutePath();
			log.debug("absolutePath :" + absolutePath);
			part.write(absolutePath);
			// unzip the zip
			final String unzipDirPath = workDir.getAbsolutePath()
				+ File.separator + save.getName();
			log.debug("unzipDirPath :" + unzipDirPath);
			try {
				Zip.unZipFolder(save, unzipDirPath);
			} catch (CompressException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			// merge the files
			final File imeiDirInUploadedFiles = new File(uploadsDirName, imei);
			if ((!(imeiDirInUploadedFiles.exists() && imeiDirInUploadedFiles
					.isDirectory()) && !imeiDirInUploadedFiles.mkdirs())) {
				log.warn("Can't create "
					+ imeiDirInUploadedFiles.getAbsolutePath());
			}
			log.debug("DOPOST");
			for (File file : new File(unzipDirPath, imei).listFiles()) {
				final File destination = new File(imeiDirInUploadedFiles,
						file.getName());
				log.debug("DOPOST joinFiles( " + destination.getAbsolutePath());
				IOCopier.joinFiles(destination, new File[] { file });
			}
			// delete the files FIXME : finally
			removeRecursive(Paths.get(unzipDirPath));
			sc.getRequestDispatcher(DATA_COLLECTION_JSP).forward(req, resp);
		}
	}

	private static final class IOCopier {

		public static void joinFiles(File destination, File[] sources)
				throws IOException {
			OutputStream output = null;
			try {
				output = createAppendableStream(destination);
				for (File source : sources) {
					appendFile(output, source);
				}
			} finally {
				IOUtils.closeQuietly(output);
			}
		}

		private static BufferedOutputStream createAppendableStream(
				File destination) throws FileNotFoundException {
			return new BufferedOutputStream(new FileOutputStream(destination,
					true));
		}

		private static void appendFile(OutputStream output, File source)
				throws IOException {
			InputStream input = null;
			try {
				input = new BufferedInputStream(new FileInputStream(source));
				IOUtils.copy(input, output);
			} finally {
				IOUtils.closeQuietly(input);
			}
		}
	}

	private static final class IOUtils {

		private static final int BUFFER_SIZE = 1024 * 4;

		public static long copy(InputStream input, OutputStream output)
				throws IOException {
			byte[] buffer = new byte[BUFFER_SIZE];
			long count = 0;
			int n = 0;
			while (-1 != (n = input.read(buffer))) {
				output.write(buffer, 0, n);
				count += n;
			}
			return count;
		}

		public static void closeQuietly(Closeable output) {
			try {
				if (output != null) {
					output.close();
				}
			} catch (IOException ioe) {
				ioe.printStackTrace();
			}
		}
	}

	// helpers
	private static String getFilename(Part part) {
		// courtesy of BalusC : http://stackoverflow.com/a/2424824/281545
		for (String cd : part.getHeader("content-disposition").split(";")) {
			if (cd.trim().startsWith("filename")) {
				String filename = cd.substring(cd.indexOf('=') + 1).trim()
						.replace("\"", "");
				return filename.substring(filename.lastIndexOf('/') + 1)
						.substring(filename.lastIndexOf('\\') + 1); // MSIE fix.
			}
		}
		return null;
	}
}
