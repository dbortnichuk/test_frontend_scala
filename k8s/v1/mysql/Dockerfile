FROM mysql:8.0.26

#ENV MYSQL_DATABASE=mysqltestdb \
#    MYSQL_ROOT_PASSWORD=pass

ADD k8s/v1/mysql/schema.sql /docker-entrypoint-initdb.d

EXPOSE 3306