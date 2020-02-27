FROM navikt/java:11

COPY build/libs/app.jar ./
COPY build/resources/main/scripts ./
RUN chmod +x run.sh
COPY run.sh /init-scripts/.
