FROM centos

WORKDIR /opt

ADD init.sh .
RUN /bin/bash init.sh

ADD run.sh .
CMD  /bin/bash run.sh

ADD checks-available /opt/checks-available
ADD checks-enabled /opt/checks-enabled

ENTRYPOINT ["/bin/bash","run.sh"]
CMD ["0" "1"]
