FROM jetty:9-jre8-alpine
USER jetty:jetty
ADD ./target/med-fhir-server.war /var/lib/jetty/webapps/root.war
COPY ./target/index /index/
EXPOSE 8080
