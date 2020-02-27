FROM navikt/java:11

COPY build/libs/app.jar ./
COPY build/resources/main/scripts /init-scripts/.

