<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
	pageEncoding="ISO-8859-1"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
<title>Data collection</title>
</head>
<body>
	<h1 style="text-align:center">Data</h1>
	<br>
	<br>
	<c:choose>
		<c:when test="${empty requestScope.master_map}">No data</c:when>
		<c:otherwise>
			<c:forEach var="master_entry" items="${master_map}">
				<center>
					<h2>
						<c:out value="${master_entry.key}" />
					</h2>
				</center>
				<c:forEach var="entry" items="${master_entry.value}">
					<center>
						<h3>
							<c:out value="${entry.key.simpleName}"></c:out>
						</h3>
					</center>
					<table>
						<c:forEach var="record" items="${entry.value}">
							<tr>
								<td><pre><c:out value="${record}" /></pre></td>
							</tr>
						</c:forEach>
					</table>
					<br>
				</c:forEach>
			</c:forEach>
		</c:otherwise>
	</c:choose>
</body>
</html>
