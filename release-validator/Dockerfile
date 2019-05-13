FROM centos

WORKDIR /opt

ADD init.sh .
ADD *.rpm .
RUN /bin/bash init.sh

EXPOSE 8080

ADD run.sh .
ENTRYPOINT /bin/bash run.sh