#FROM openliberty/open-liberty:full-java11-openj9-ubi
FROM openliberty/open-liberty:kernel-slim-java11-openj9-ubi

ARG VERSION=1.1.8
ARG REVISION=RELEASE

LABEL \
  org.opencontainers.image.authors="Nathaniel Mills" \
  org.opencontainers.image.vendor="IBM" \
  org.opencontainers.image.url="local" \
  org.opencontainers.image.source="https://github.com/IBM/WhitelistMasker/tree/master/MaskWebServices" \
  org.opencontainers.image.version="$VERSION" \
  org.opencontainers.image.revision="$REVISION" \
  vendor="IBM" \
  name="MaskWebServices" \
  version="$VERSION-$REVISION" \
  summary="MaskWebServices" \
  description="This image contains the Open Source MaskWebServices microservice running with the Open Liberty runtime."

COPY --chown=1001:0 MaskWebServices/server.xml /config/

RUN features.sh

COPY --chown=1001:0 MaskWebServices/target/*.war /config/dropins/
COPY --chown=1001:0 MaskWebServices/properties/  /opt/ol/wlp/output/defaultServer/properties/
# COPY --chown=1001:0 MaskWebServices/server.env /config/
COPY --chown=1001:0 Masker/properties/  /opt/ol/wlp/output/defaultServer/properties/

# copy the .jar containing the utility into the appropriate place (relative to properties directory)
COPY --chown=1001:0 Masker/target/Masker-1.1.8-jar-with-dependencies.jar /opt/ol/wlp/output/defaultServer/Masker-1.1.8-jar-with-dependencies.jar

# Pick up security fixes
USER root
RUN yum clean all --disableplugin=subscription-manager && \
    yum -y update --disableplugin=subscription-manager && \
    yum clean all --disableplugin=subscription-manager && \
    rm -rf /var/cache/yum /tmp/* /var/tmp/*
    
ENV AIDEN_HOME=/opt/ol/wlp/output/defaultServer

RUN configure.sh
