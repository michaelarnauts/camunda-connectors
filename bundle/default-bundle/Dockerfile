FROM eclipse-temurin:21.0.1_12-jre

VOLUME /tmp

# The /opt/app is used for the Connectors runtime, and out-of-the-box connectors
# Use the /opt/custom to mount your own connectors, secret providers, or include other jars into the classpath
RUN mkdir /opt/app && mkdir /opt/custom

COPY target/*-with-dependencies.jar /opt/app/

# Using the start script from the base connector runtime image
COPY start.sh /start.sh
RUN chmod +x start.sh

# Create an unprivileged user / group and switch to that user
RUN addgroup --gid 1001 camunda && useradd --no-create-home --gid 1001 --uid 1001 camunda
USER 1001:1001

ENV ZEEBE_CLIENT_CONFIG_PATH=/tmp/connectors
ENTRYPOINT ["/start.sh"]
