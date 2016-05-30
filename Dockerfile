FROM ubuntu:16.04

# --- --- ---  Proxy Settings --- --- ---
#UNCOMMENT IF BHEIND A PROXY
#SET A PROPER PROXY IP

ENV DOCKER_PROXY http://10.124.55.166:3128

ENV http_proxy ${DOCKER_PROXY}
ENV HTTP_PROXY ${DOCKER_PROXY}
ENV https_proxy ${DOCKER_PROXY}
ENV HTTPS_PROXY ${DOCKER_PROXY}
ENV NO_PROXY '127.0.0.1, localhost, /var/run/docker.sock'

ENV SCALA_VERSION 2.11.8
ENV SBT_VERSION 0.13.11

RUN apt-get update && \
    apt-get install -y \
    wget tar curl git \
    vim make \
    python \
    openjdk-8-jdk \
    build-essential \
    libkrb5-dev \
    screen

ENV JAVA_HOME /usr/lib/jvm/java-1.8.0-openjdk

RUN \
  curl -fsL http://downloads.typesafe.com/scala/$SCALA_VERSION/scala-$SCALA_VERSION.tgz | tar xfz - -C /root/ && \
  echo >> /root/.bashrc && \
  echo 'export PATH=~/scala-$SCALA_VERSION/bin:$PATH' >> /root/.bashrc

RUN \
  curl -L -o sbt-$SBT_VERSION.deb http://dl.bintray.com/sbt/debian/sbt-$SBT_VERSION.deb && \
  dpkg -i sbt-$SBT_VERSION.deb && \
  rm sbt-$SBT_VERSION.deb

RUN apt-get update && \
    apt-get -y install sbt

RUN curl -sL https://deb.nodesource.com/setup_5.x | bash -

RUN apt-get install -y nodejs

WORKDIR /home

RUN git clone https://github.com/krishnasrinivas/wetty.git

WORKDIR /home/wetty

RUN npm install

RUN useradd -d /home/disruptor_talk -m -s /bin/bash term

RUN echo 'term:term' | chpasswd

WORKDIR /home

RUN rm -rf ./disruptor_talk

RUN git clone https://github.com/andreaTP/disruptor_talk

WORKDIR /home/disruptor_talk/code

RUN sbt compile

WORKDIR /home/disruptor_talk

EXPOSE 3000 8000

RUN echo "root:root" | chpasswd

CMD ["/bin/bash"]
