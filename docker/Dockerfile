FROM debian:bookworm-slim

USER root

ENV USER=cafeyike
# Home dir
ENV HOME=/home/$USER
# Virtual env location
ENV VENV=$HOME/venv
# venv python exec
ENV PY=${VENV}/bin/python3
ARG YIKE_JAR

RUN apt-get update && \
    apt-get install -y ca-certificates-java && \
    apt-get install -y python3-pip python3-venv openjdk-17-jre-headless && \
    apt-get clean

RUN adduser --system $USER --home $HOME

USER $USER

# Store the venv location
ADD .bashrc $HOME/.bashrc
RUN echo "export CafeYikeJar=$YIKE_JAR" >> $HOME/.bashrc

# Add python reqs
ADD build/requirements.txt $HOME/setup/
# Install py dependencies
RUN python3 -m venv $VENV && \
    $PY -m pip install --upgrade pip && \
    $PY -m pip install setuptools wheel && \
    $PY -m pip install -r $HOME/setup/requirements.txt

# Create dirs with the corrent permissions
RUN mkdir $HOME/dat

# Add start scripts
ADD --chown=$USER build/start_java.sh $HOME/
ADD --chown=$USER start_manager.sh $HOME/
RUN chmod +x $HOME/*.sh
# Add frontend
COPY --chown=$USER build/ym-frontend/ $HOME/frontend/
# Add python reqs
ADD --chown=$USER build/requirements.txt $HOME/setup/
# Add yikemng
ADD --chown=$USER build/yikemng-1.0.0-py3-none-any.whl $HOME/setup/
# Add CafeYike
ADD --chown=$USER build/CafeYike*.jar $HOME/

# Install yikemng
RUN $PY -m pip install $HOME/setup/yikemng-1.0.0-py3-none-any.whl

