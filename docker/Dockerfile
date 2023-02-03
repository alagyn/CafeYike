ARG baseOS
FROM ${baseOS}

USER root

RUN apt-get update
RUN apt-get install -y python3-pip openjdk-11-jdk-headless

USER pi

# Add python reqs
ADD build/requirements.txt /home/pi/setup/
# Install py dependencies
RUN python3 -m pip install --upgrade pip
RUN python3 -m pip install setuptools wheel
RUN python3 -m pip install -r /home/pi/setup/requirements.txt

# Create dirs with the corrent permissions
RUN mkdir /home/pi/dat

# Add start scripts
ADD --chown=pi build/start_java.sh /home/pi/
ADD --chown=pi start_manager.sh /home/pi/
RUN chmod +x /home/pi/*.sh
# Add frontend
COPY --chown=pi build/ym-frontend/ /home/pi/frontend/
# Add python reqs
ADD --chown=pi build/requirements.txt /home/pi/setup/
# Add yikemng
ADD --chown=pi build/yikemng-1.0.0-py3-none-any.whl /home/pi/setup/
# Add CafeYike
ADD --chown=pi build/CafeYike*.jar /home/pi/
# Add system.conf
ADD --chown=pi build/system.conf /home/pi/

# Install yikemng
RUN python3 -m pip install /home/pi/setup/yikemng-1.0.0-py3-none-any.whl

