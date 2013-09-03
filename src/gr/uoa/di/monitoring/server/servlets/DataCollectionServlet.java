package gr.uoa.di.monitoring.server.servlets;

import gr.uoa.di.monitoring.model.Battery;

import java.io.IOException;
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
			req.setAttribute("key", part.getName());
			sc.getRequestDispatcher(DATA_COLLECTION_JSP).forward(req, resp);
		}
	}
}
