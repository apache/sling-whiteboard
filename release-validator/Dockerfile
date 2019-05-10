FROM store/oracle/serverjre:8

WORKDIR /opt

ADD init.sh .
RUN /bin/bash init.sh

EXPOSE 8080

ADD run.sh .
ENTRYPOINT /bin/bash run.sh