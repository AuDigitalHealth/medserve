FROM jetty:9-jre8-alpine
USER jetty:jetty
ADD ./target/${JAR_FILE} /var/lib/jetty/webapps/root.war
COPY ./target/index /index/
EXPOSE 8080
