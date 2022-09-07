FROM java:8
MAINTAINER 0xyk3r<anjiongyi@163.com>
ADD ./target/Strix-1.0.0.jar app.jar
CMD java -jar -server -Xms1024m -Xmx1024m app.jar --spring.profiles.active=prod
