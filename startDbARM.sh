docker run -d --rm -p 3306:3306 --name mysql --volume ~/Desktop/db/mysql:/var/lib/mysql -e MYSQL_ROOT_PASSWORD=root mysql/mysql-server:latest-aarch64
