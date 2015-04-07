<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<html>
<body>
This is homepage jsp and <br/>
SESSION ID IS:${session.id} with ${fn:length(session.attributeNames)} attributes<br/>

${pageContext.include("/include")}

</body>
</html>
