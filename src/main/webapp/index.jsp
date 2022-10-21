
<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%
    response.setStatus(response.SC_MOVED_TEMPORARILY);
    response.setHeader("Location", request.getContextPath()+"/servlet"); 
%>
