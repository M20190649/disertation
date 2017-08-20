TigerVNC

install tigerVncL http://c-nergy.be/blog/?p=9962

Make password less than 5 characters
/home/ubuntu/.vnc# vncpasswd -f > sesman_ubuntu_passwd

Make rdp fast: change backround + disable animations:
https://forum.xfce.org/viewtopic.php?id=5363
https://askubuntu.com/questions/689602/disable-all-visual-effects-in-ubuntu-15-10


SSH

chmod 600 /path/to/private.pem

In  ~/.ssh/config (set universal credentials):
User ubuntu
IdentityFile /path/to/private.pem


Spark
https://spark.apache.org/docs/2.1.0/spark-standalone.html

./sbin/start-master.sh
./sbin/start-slave.sh  spark://<spark_url>:7077

sbin/stop-master.sh
sbin/stop-slaves.sh - see conf/slaves
sbin/stop-all.sh

configure: conf/spark-env.sh


Barefot

Mapserver Docker container name: MapServerContainer 

Jenkins
Port: 9999 -> You can go to /etc/default/jenkins and add --httpPort=9999 or whatever port to JENKINS_ARGS. sudo systemctl restart jenkins


Credentials:
User: ubuntu
Password: 123



Params matching server
https://github.com/bmwcarit/barefoot/wiki#parameters



Submit app:
./bin/spark-submit   --class TrafficAnalyticsApp --master spark://52.201.248.251:7077   --executor-memory 1G   --total-executor-cores 10 --verbose  ~/traffic-analyzer-project-1.0-jar-with-dependencies.jar in out 127.0.0.1 5432 Disertation ubuntu 123 ~/road-types.json ~/traces.csv


Postgres sql database name: Disertation; user: ubuntu; pass: 123 ?



Docker:

sudo docker start MapServerContainer
sudo docker attach MapServerContainer


psql -h 127.0.0.1 -p 5432 -d Disertation --username=ubuntu

bash /mnt/map/osm/import.sh /mnt/map/osm/SanFrancisco.osm.pbf Disertation ubuntu 123 /mnt/map/tools/road-types.json slim

