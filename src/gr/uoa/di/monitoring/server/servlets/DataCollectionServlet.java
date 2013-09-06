package gr.uoa.di.monitoring.server.servlets;

import gr.uoa.di.monitoring.model.Battery;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;

@SuppressWarnings("serial")
@WebServlet("/")
@MultipartConfig()
public final class DataCollectionServlet extends Controller {

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		// TODO forbidden !
		req.setAttribute("key", Battery.hallo());
		sc.getRequestDispatcher(DATA_COLLECTION_JSP).forward(req, resp);
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		Collection<Part> parts = req.getParts();
		for (Part part : parts) {
			String sRootPath = new File("").getAbsolutePath();
			File save = new File(sRootPath, getFilename(part) + "_"
				+ System.currentTimeMillis());
			log.debug(save.getAbsolutePath());
			InputStream filecontent = part.getInputStream();
			FileOutputStream f = new FileOutputStream(save);
			int read = 0;
			byte[] bytes = new byte[1024];
			while ((read = filecontent.read(bytes)) != -1) {
				f.write(bytes, 0, read);
			}
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
