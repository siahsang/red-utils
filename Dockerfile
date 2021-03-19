FROM alpine:3.7

LABEL maintainer="Javad Alimohammadi "

# Install system dependencies
RUN apk update
RUN apk add wget
RUN apk add make
RUN apk add gcc
RUN apk add musl-dev
RUN apk add linux-headers
RUN apk add tcl

# Get last stable redis version
WORKDIR /home
RUN wget -O redis.tar.gz  http://download.redis.io/redis-stable.tar.gz
RUN tar xfz redis.tar.gz
RUN rm redis.tar.gz
RUN mv redis-* redis
WORKDIR /home/redis
RUN make
RUN cp /home/redis/src/redis-server /usr/local/bin/
RUN cp /home/redis/src/redis-cli /usr/local/bin/
ENTRYPOINT ["redis-server"]
