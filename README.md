# CRaC Example application

For more info, check:

* [What is CRaC?](https://docs.azul.com/core/crac/crac-introduction)
* [CRaC Usage Guidelines](https://docs.azul.com/core/crac/crac-guideline)
* About this example application: [Step-by-step CRaC support for a Jetty app](https://github.com/CRaC/docs/blob/master/STEP-BY-STEP.md)

## Manual steps

```
docker build -t zulu-crac .

./mvnw clean package
java -jar target/example-jetty-1.0-SNAPSHOT.jar
curl -i http://localhost:8080

docker run --privileged -it --rm -p 8080:8080 -v $PWD:/opt/mnt --name example-jetty-crac zulu-crac bash

echo 128 > /proc/sys/kernel/ns_last_pid
java -XX:CRaCCheckpointTo=/opt/mnt/crac -jar /opt/mnt/target/example-jetty-1.0-SNAPSHOT.jar

docker exec -it example-jetty-crac bash
jcmd 129 JDK.checkpoint

java -XX:CRaCRestoreFrom=/opt/mnt/crac

docker build -t example-jetty-crac-restore -f Dockerfile.restore .
docker run --rm -p 8080:8080 --name example-jetty-crac-restore example-jetty-crac-restore
```
