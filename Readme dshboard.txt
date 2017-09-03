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


Spark:
https://spark.apache.org/docs/2.1.0/spark-standalone.html

./sbin/start-master.sh
./sbin/start-slave.sh  spark://<spark_url>:7077

./sbin/start-slave.sh spark://ip-172-31-7-204:7077

sbin/stop-master.sh
sbin/stop-slaves.sh - see conf/slaves
sbin/stop-all.sh

configure: conf/spark-env.sh


Kafka:

-- Configure Kafka

bin/zookeeper-server-start.sh config/zookeeper.properties

bin/kafka-server-start.sh config/server.properties -
	# The port the socket server listens on
	port=9092

	# Hostname the broker will bind to. If not set, the server will bind to all interfaces
	host.name=192.168.1.131



bin/kafka-run-class.sh kafka.tools.ConsumerOffsetChecker --zkconnect localhost:2181 --group test

bin/kafka-console-consumer.sh --zookeeper localhost:2181 --topic test --from-beginning

bin/kafka-console-producer.sh --broker-list localhost:9092 --topic test

bin/kafka-topics.sh --list --zookeeper localhost:2181


bin/kafka-topics.sh --create --zookeeper localhost:2181 --replication-factor 1 --partitions 1 --topic test




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

// carefull with the toal-executor-cores - it's per cluster
./bin/spark-submit   --class DashboardAnalyticsApp --master spark://34.233.214.65:7077  --driver-cores 2 --executor-memory 8G   --total-executor-cores 8 --verbose  "/home/ubuntu/Desktop/disertation/Spark Application/target/analytics-project-1.0-jar-with-dependencies.jar" Noise

Postgres sql database name: Disertation; user: ubuntu; pass: 123 ?



Docker:

sudo docker start MapServerContainer
sudo docker attach MapServerContainer


psql -h 127.0.0.1 -p 5432 -d Disertation --username=ubuntu

bash /mnt/map/osm/import.sh /mnt/map/osm/SanFrancisco.osm.pbf Disertation ubuntu 123 /mnt/map/tools/road-types.json slim


http://benrobb.com/2007/01/15/howto-remote-root-access-to-mysql/



Kill hanging mongo ports:
sudo fuser -k Port 27017/tcp
sudo iptables -t filter -A INPUT -p tcp -i eth0 -m tcp --sport 27017 -j DROP

!!!! Always clear kafka logs before starting tests; it seems that it gets very slow otherwise
Clear kafka logs
rm -R /tmp/kafka-logs 


date && java -jar analytics-dashboard-0.1.0-jar-with-dependencies.jar Temperature 60000 && date
date && java -jar analytics-dashboard-0.1.0-jar-with-dependencies.jar Traffic 60000 ~/rawdata/traces/sanfranciscocabs && date

start server agent  ~/Desktop/ServerAgent-2.2.1/startAgent.sh


/home/ubuntu/Desktop/disertation/Dashboard/src/main/assets/
