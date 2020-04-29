FROM navikt/java:11-appdynamics

COPY build/libs/app.jar ./
COPY build/resources/main/scripts /init-scripts/.

