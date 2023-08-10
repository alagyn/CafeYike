FROM debian:bookworm-slim

USER root

# Virtual env location
ENV VENV=/home/root/venv
# venv python exec
ENV PY=${VENV}/bin/python3

RUN apt-get update && \
    apt-get install -y ca-certificates-java && \
    apt-get install -y python3-pip python3-venv openjdk-17-jre-headless && \
    apt-get clean

# Store the venv location
RUN mkdir /home/root && echo "export PY=$PY" > /home/root/.bashrc

# Add python reqs
ADD build/requirements.txt /home/root/setup/
# Install py dependencies
RUN python3 -m venv $VENV && \
    $PY -m pip install --upgrade pip && \
    $PY -m pip install setuptools wheel && \
    $PY -m pip install -r /home/root/setup/requirements.txt

# Create dirs with the corrent permissions
RUN mkdir /home/root/dat

# Add start scripts
ADD --chown=root build/start_java.sh /home/root/
ADD --chown=root start_manager.sh /home/root/
RUN chmod +x /home/root/*.sh
# Add frontend
COPY --chown=root build/ym-frontend/ /home/root/frontend/
# Add python reqs
ADD --chown=root build/requirements.txt /home/root/setup/
# Add yikemng
ADD --chown=root build/yikemng-1.0.0-py3-none-any.whl /home/root/setup/
# Add CafeYike
ADD --chown=root build/CafeYike*.jar /home/root/
# Add system.conf
ADD --chown=root build/system.conf /home/root/

# Install yikemng
RUN $PY -m pip install /home/root/setup/yikemng-1.0.0-py3-none-any.whl

