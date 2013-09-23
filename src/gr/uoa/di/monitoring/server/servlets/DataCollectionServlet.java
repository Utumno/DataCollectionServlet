package gr.uoa.di.monitoring.server.servlets;

import gr.uoa.di.monitoring.android.persist.FileStore;
import gr.uoa.di.monitoring.model.Battery;
import gr.uoa.di.monitoring.model.Position.LocationFields;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumMap;
import java.util.List;

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
		System.out.println(Battery.hallo());
		List<EnumMap<LocationFields, List<Byte>>> list = FileStore
				.getEntriesLoc(new FileInputStream(
						"C:/_/logs/354957034870710/loc"));
		req.setAttribute("key", Arrays.toString(list.toArray()));
		sc.getRequestDispatcher(DATA_COLLECTION_JSP).forward(req, resp);
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		Collection<Part> parts = req.getParts();
		for (Part part : parts) {
			File save = new File(uploadsDirName, getFilename(part) + "_"
				+ System.currentTimeMillis() + ".zip");
			final String absolutePath = save.getAbsolutePath();
			log.debug(absolutePath);
			part.write(absolutePath);
			sc.getRequestDispatcher(DATA_COLLECTION_JSP).forward(req, resp);
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
