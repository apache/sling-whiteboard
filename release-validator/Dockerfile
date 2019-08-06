FROM centos

WORKDIR /opt

ADD init.sh .
RUN /bin/bash init.sh

EXPOSE 8080

ADD run.sh .
CMD  /bin/bash run.sh

ENTRYPOINT ["/bin/bash","run.sh"]
CMD ["0"]