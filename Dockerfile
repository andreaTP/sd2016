FROM ubuntu:16.04

# --- --- ---  Proxy Settings --- --- ---
#UNCOMMENT IF BHEIND A PROXY
#SET A PROPER PROXY IP

#ENV DOCKER_PROXY PROXY_IP

#ENV http_proxy ${DOCKER_PROXY}
#ENV HTTP_PROXY ${DOCKER_PROXY}
#ENV https_proxy ${DOCKER_PROXY}
#ENV HTTPS_PROXY ${DOCKER_PROXY}
#ENV NO_PROXY '127.0.0.1, localhost, /var/run/docker.sock'

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

RUN useradd -d /home/sd2016 -m -s /bin/bash term

RUN echo 'term:term' | chpasswd

WORKDIR /home

RUN git clone https://github.com/scala-js/scala-js.git

WORKDIR /home/scala-js

RUN sbt ";++2.11.8;compiler/publishLocal;library/publishLocal;javalibEx/publishLocal;testInterface/publishLocal;stubs/publishLocal;jasmineTestFramework/publishLocal;jUnitRuntime/publishLocal;jUnitPlugin/publishLocal"

RUN sbt ";++2.10.6;ir/publishLocal;tools/publishLocal;jsEnvs/publishLocal;jsEnvsTestKit/publishLocal;testAdapter/publishLocal;sbtPlugin/publishLocal"

WORKDIR /home

RUN git clone https://github.com/unicredit/akka.js.git

WORKDIR /home/akka.js

RUN git checkout refactoring

RUN git submodule init

RUN git submodule update

RUN sbt akkaActorJSIrPatches/compile

RUN sbt akkaActorJS/publishLocal

WORKDIR /home

RUN rm -rf /home/sd2016/

RUN git clone https://github.com/andreaTP/sd2016

WORKDIR /home/sd2016

RUN git pull

WORKDIR /home/sd2016/code5

RUN npm install websocket

WORKDIR /home/sd2016/code1

RUN sbt compile

WORKDIR /home/sd2016/code2

RUN sbt compile

WORKDIR /home/sd2016/code3

RUN sbt fullOptJS

WORKDIR /home/sd2016/code4

RUN sbt fullOptJS

WORKDIR /home/sd2016/code5

RUN sbt compile
RUN sbt fullOptJS

WORKDIR /home/sd2016/code6

RUN sbt fullOptJS

WORKDIR /home/sd2016

#RUN git pull

EXPOSE 3000 8000 9001 9002

RUN echo "root:root" | chpasswd

CMD ["/bin/bash"]
