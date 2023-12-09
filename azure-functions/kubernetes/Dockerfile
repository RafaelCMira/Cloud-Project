FROM openjdk:17-jdk-slim

RUN mkdir functions

WORKDIR /usr/local/functions

ADD scc2324-1.0.jar scc2324-1.0.jar


ENTRYPOINT ["java", "-jar", "scc2324-1.0.jar", "scc.serverless.TimerFunction"]