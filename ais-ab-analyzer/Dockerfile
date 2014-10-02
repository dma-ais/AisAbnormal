FROM tbsalling/javabox
VOLUME /data
ADD Docker-start.sh /start.sh
RUN cd /; wget --progress=bar https://dma.ci.cloudbees.com/job/AisAbnormal/lastSuccessfulBuild/artifact/ais-ab-analyzer/target/ais-ab-analyzer-0.1-SNAPSHOT-bundle.zip; unzip ais-ab-analyzer-0.1-SNAPSHOT-bundle.zip
CMD ["/bin/bash", "/start.sh"]
