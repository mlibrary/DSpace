# This image will be published as dspace/dspace-dependencies
# The purpose of this image is to make the build for dspace/dspace run faster

# Step 1 - Run Maven Build
FROM maven:3.9-eclipse-temurin-17 AS build
ARG TARGET_DIR=dspace-installer
WORKDIR /app
# Create the 'dspace' user account & home directory
RUN useradd dspace \
    && mkdir -p /home/dspace \
    && chown -Rv dspace: /home/dspace
RUN chown -Rv dspace: /app
# Need git to support buildnumber-maven-plugin, which lets us know what version of DSpace is being run.
RUN apt-get update \
    && apt-get install -y --no-install-recommends git \
    && apt-get purge -y --auto-remove \
    && rm -rf /var/lib/apt/lists/*

# Switch to dspace user & run below commands as that user
USER dspace

# Copy the DSpace source code (from local machine) into the workdir (excluding .dockerignore contents)
ADD --chown=dspace . /app/

# Trigger the installation of all maven dependencies (hide download progress messages)
RUN mvn --no-transfer-progress package -Pdspace-rest

# Clear the contents of the /app directory (including all maven builds), so no artifacts remain.
# This ensures when dspace:dspace is built, it will use the Maven local cache (~/.m2) for dependencies
USER root
RUN rm -rf /app/*
