FROM dspace/dspace-postgres-pgcrypto:dspace-7_x

ARG UID=1000950000

#USER root
#
#RUN useradd -g root -m -s /bin/bash -l -o -u $UID postgres
#
#USER postgres

RUN usermod -u $UID postgres
