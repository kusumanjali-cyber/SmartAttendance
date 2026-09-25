FROM tomcat:9.0-jdk11-temurin

RUN rm -rf /usr/local/tomcat/webapps/*
RUN mkdir -p /usr/local/tomcat/webapps/SmartAttendance

# HTML files
COPY index.html /usr/local/tomcat/webapps/SmartAttendance/
COPY style.css /usr/local/tomcat/webapps/SmartAttendance/
COPY script.js /usr/local/tomcat/webapps/SmartAttendance/
COPY home.html /usr/local/tomcat/webapps/SmartAttendance/
COPY login.html /usr/local/tomcat/webapps/SmartAttendance/
COPY admin.html /usr/local/tomcat/webapps/SmartAttendance/
COPY faculty.html /usr/local/tomcat/webapps/SmartAttendance/
COPY student.html /usr/local/tomcat/webapps/SmartAttendance/
COPY WEB-INF /usr/local/tomcat/webapps/SmartAttendance/WEB-INF/

# MySQL Connector
COPY lib/mysql-connector-j-26.7.0.jar /usr/local/tomcat/lib/

# Create classes folder
RUN mkdir -p /usr/local/tomcat/webapps/SmartAttendance/WEB-INF/classes

# Compile AttendanceServlet
COPY src/AttendanceServlet.java /tmp/AttendanceServlet.java

RUN javac -cp "/usr/local/tomcat/lib/servlet-api.jar:/usr/local/tomcat/lib/mysql-connector-j-26.7.0.jar" \
    -d /usr/local/tomcat/webapps/SmartAttendance/WEB-INF/classes \
    /tmp/AttendanceServlet.java

# Compile LoginServlet
COPY src/LoginServlet.java /tmp/LoginServlet.java

RUN javac -cp "/usr/local/tomcat/lib/servlet-api.jar:/usr/local/tomcat/lib/mysql-connector-j-26.7.0.jar" \
    -d /usr/local/tomcat/webapps/SmartAttendance/WEB-INF/classes \
    /tmp/LoginServlet.java

EXPOSE 8080

CMD ["catalina.sh", "run"]