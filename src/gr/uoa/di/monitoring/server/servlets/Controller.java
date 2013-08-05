package gr.uoa.di.monitoring.server.servlets;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("serial")
class Controller extends HttpServlet {

	static ServletContext sc;
	Logger log;

	@Override
	public void init() throws ServletException {
		super.init();
		if (sc == null) sc = getServletContext();
		log = LoggerFactory.getLogger(this.getClass());
	}
}
