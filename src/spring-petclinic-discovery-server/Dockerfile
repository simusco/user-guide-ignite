FROM java:8-jdk
MAINTAINER Elvis Zhang

ADD ./target/spring-petclinic-discovery-server-1.5.4.jar /opt/
CMD ["java", "-Xmx200m", "-jar", "/opt/spring-petclinic-discovery-server-1.5.4.jar"]