package gr.uoa.di.monitoring.server.servlets;

import gr.uoa.di.java.helpers.Zip;
import gr.uoa.di.java.helpers.Zip.CompressException;
import gr.uoa.di.monitoring.android.files.Parser;
import gr.uoa.di.monitoring.android.files.ParserException;
import gr.uoa.di.monitoring.model.Data;

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

	private static final String UPLOAD_LOCATION_PROPERTY_KEY = "upload.location";
	private volatile String uploadsDirName;

	@Override
	public void init() throws ServletException {
		super.init();
		if (uploadsDirName == null) {
			uploadsDirName = property(UPLOAD_LOCATION_PROPERTY_KEY);
			final File uploadsDir = new File(uploadsDirName);
			if (!uploadsDir.isDirectory() && !uploadsDir.mkdirs()) {
				throw new ServletException("Unable to create "
					+ uploadsDir.getAbsolutePath() + " data upload directory");
			}
		}
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
						Parser.parse(node.getAbsolutePath()));
				} catch (ParserException e) {
					log.warn("Failed to parse " + node.getAbsolutePath(), e);
				}
			}
		}
		req.setAttribute("master_map", master_map);
		sc.getRequestDispatcher(DATA_COLLECTION_JSP).forward(req, resp);
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException {
		Collection<Part> parts;
		try {
			parts = req.getParts();
		} catch (IllegalStateException | IOException e) {
			log.error("Can't get file parts from " + req, e);
			return;
		}
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
			File uploadedFile = new File(uploadsDirName, filename + "_"
				+ System.currentTimeMillis() + ".zip");
			final String imei = Parser.getDeviceID(filename);
			final String absolutePath = uploadedFile.getAbsolutePath();
			log.debug("absolutePath :" + absolutePath);
			try {
				part.write(absolutePath);
			} catch (IOException e) {
				log.error("Can't write file " + absolutePath + " from part "
					+ part, e);
				return;
			}
			// unzip the zip
			final String unzipDirPath = workDir.getAbsolutePath()
				+ File.separator + uploadedFile.getName();
			log.debug("unzipDirPath :" + unzipDirPath);
			try {
				Zip.unZipFolder(uploadedFile, unzipDirPath);
			} catch (CompressException e) {
				log.error("Can't unzip file " + absolutePath, e);
			}
			// merge the files
			final File imeiDirInUploadedFiles = new File(uploadsDirName, imei);
			if (!imeiDirInUploadedFiles.isDirectory()
				&& !imeiDirInUploadedFiles.mkdirs()) {
				log.error("Can't create "
					+ imeiDirInUploadedFiles.getAbsolutePath());
				return;
			}
			final File unzipedFolder = new File(unzipDirPath, imei); // get this
			// from FileStore
			for (File file : unzipedFolder.listFiles()) {
				final File destination = new File(imeiDirInUploadedFiles,
					file.getName());
				try {
					IOCopier.joinFiles(destination, new File[] { file });
				} catch (IOException e) {
					log.error("Failed to append " + file.getAbsolutePath()
						+ " to " + destination.getAbsolutePath());
					return;
				}
			}
			try {
				removeRecursive(Paths.get(unzipDirPath));
			} catch (IOException e) {
				String msg = "Failed to delete folder " + unzipDirPath;
				if (e instanceof java.nio.file.DirectoryNotEmptyException) {
					msg += ". Still contains : ";
					final File[] listFiles = Paths.get(unzipDirPath).toFile()
						.listFiles();
					if (listFiles != null) for (File file : listFiles) {
						msg += file.getAbsolutePath() + "\n";
					}
				}
				log.error(msg, e);
			}
			// FIXME : http://stackoverflow.com/questions/19935624/
			// java-nio-file-files-deletepath-path-will-always-throw-on-failure
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
