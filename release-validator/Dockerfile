FROM centos

WORKDIR /opt
ENV JDK_RPM=jdk-8u211-linux-x64.rpm

ADD init.sh .
ADD $JDK_RPM .
RUN /bin/bash init.sh

EXPOSE 8080

ADD run.sh .
CMD  /bin/bash run.sh

ENTRYPOINT ["/bin/bash","run.sh"]
CMD ["0"]