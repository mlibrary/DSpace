# UM DEV

## Clone

```
git clone https://github.com/mlibrary/DSpace.git
cd DSpace
git checkout umich
```

## Create dspace/config/local.cfg

```
dspace.dir=/dspace
dspace.server.url = http://localhost:8080/server
dspace.ui.url = http://localhost:4000
dspace.name = DSpace at My University
db.url = jdbc:postgresql://localhost:5432/dspace
db.driver = org.postgresql.Driver
db.dialect = org.hibernate.dialect.PostgreSQL94Dialect
db.username = dspace
db.password = dspace
db.schema = public
rest.cors.allowed-origins = ${dspace.ui.url}
filestorage.dir=data
pubmed.apiKey=CHANGEME
ip.umIPs = 141.211.|35.2.
ip.bioIPsRange1 = 10.0.0.1|10.0.0.254
ip.bioIPsRange2 = 10.0.1.1|10.0.1.254
ip.BentleyOnlyIPs = 141.211.1.
oai.config.dir = ${dspace.dir}/config/crosswalks/oai
oai.description.file = ${dspace.dir}/config/crosswalks/oai/oai.cfg
oai.cache.dir = ${dspace.dir}/var/oai
```

## Docker Compose

```
docker compose build
docker compose up -d
```

## Sanity Check
```
curl -i http://localhost:8080/server/api
```

## Create Administrator
```
docker compose exec -- dspace bash
WARN[0000] /Users/gkostin/GitHub/mlibrary/DSpace/docker-compose.yml: the attribute `version` is obsolete, it will be ignored, please remove it to avoid potential confusion
```

```
cd /dspace/bin
./dspace create-administrator
Creating an initial administrator account
E-mail address: gkostin@umich.edu
First name: Greg
Last name: Kostin
Is the above data correct? (y or n): y
Password will not display on screen.
Password: 
Again to confirm: 
Administrator account created
```
