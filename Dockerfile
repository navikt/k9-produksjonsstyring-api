FROM navikt/java:11-appdynamics
ENV APPD_ENABLED=true

COPY build/libs/app.jar ./
COPY build/resources/main/scripts /init-scripts/.

