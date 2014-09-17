FROM tbsalling/javabox
VOLUME /data
EXPOSE 80
ADD Docker-start.sh /start.sh
RUN git clone https://github.com/dma-ais/AisAbnormal.git /AisAbnormal
RUN cd /AisAbnormal; git checkout release; mvn install; cd ais-ab-web/target; unzip ais-ab-web-0.1-SNAPSHOT-bundle.zip
CMD ["/bin/bash", "/start.sh"]
