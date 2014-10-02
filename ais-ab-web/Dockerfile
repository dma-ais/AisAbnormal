FROM tbsalling/javabox
VOLUME /data
EXPOSE 80
ADD Docker-start.sh /start.sh
RUN cd /; wget --progress=dot:mega https://dma.ci.cloudbees.com/job/AisAbnormal/lastSuccessfulBuild/artifact/ais-ab-web/target/ais-ab-web-0.1-SNAPSHOT-bundle.zip; unzip ais-ab-web-0.1-SNAPSHOT-bundle.zip
CMD ["/bin/bash", "/start.sh"]
