FROM alpine:3.7

LABEL maintainer="Javad Alimohammadi <bs.alimohammadi@gmail.com>"

# Install system dependencies
RUN apk update
RUN apk add bash
RUN apk add wget
RUN apk add make
RUN apk add gcc
RUN apk add musl-dev
RUN apk add linux-headers
RUN apk add tcl


# Get the last stable version of redis
WORKDIR /home
RUN wget -O redis.tar.gz  http://download.redis.io/redis-stable.tar.gz
RUN tar xfz redis.tar.gz
RUN rm redis.tar.gz
RUN mv redis-* redis
WORKDIR /home/redis
RUN make
RUN cp /home/redis/src/redis-server /usr/local/bin/
RUN cp /home/redis/src/redis-cli /usr/local/bin/

COPY docker-entrypoint.sh /docker-entrypoint.sh

EXPOSE 6379
ENTRYPOINT ["/docker-entrypoint.sh", "--protected-mode no"]


#COPY redis-cluster.tmpl /redis-conf/redis-cluster.tmpl
#COPY redis.tmpl         /redis-conf/redis.tmpl
#COPY sentinel.tmpl      /redis-conf/sentinel.tmpl
#
## Add startup script
#COPY docker-entrypoint.sh /docker-entrypoint.sh
#
## Add script that generates supervisor conf file based on environment variables
#COPY generate-supervisor-conf.sh /generate-supervisor-conf.sh
#
#RUN chmod 755 /docker-entrypoint.sh
#
#EXPOSE 7000 7001 7002 7003 7004 7005 7006 7007 5000 5001 5002
#
#ENTRYPOINT ["/docker-entrypoint.sh"]
#CMD ["redis-cluster"]