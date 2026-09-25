FROM tomcat:9.0-jdk8-temurin

RUN rm -rf /usr/local/tomcat/webapps/*

# Copy all website files
COPY *.html /usr/local/tomcat/webapps/ROOT/
COPY *.css /usr/local/tomcat/webapps/ROOT/
COPY *.js /usr/local/tomcat/webapps/ROOT/

# Copy complete WEB-INF
COPY WEB-INF /usr/local/tomcat/webapps/ROOT/WEB-INF/

# Copy all required JAR files
COPY lib/*.jar /usr/local/tomcat/lib/

EXPOSE 8080

CMD ["sh", "-c", "sed -i \"s/port=\\\"8080\\\"/port=\\\"${PORT:-10000}\\\"/\" /usr/local/tomcat/conf/server.xml && catalina.sh run"]