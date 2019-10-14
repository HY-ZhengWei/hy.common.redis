#!/bin/sh

mvn deploy:deploy-file -Dfile=hy.common.redis.jar                              -DpomFile=./src/META-INF/maven/org/hy/common/redis/pom.xml -DrepositoryId=thirdparty -Durl=http://218.21.3.19:1481/nexus/content/repositories/thirdparty
mvn deploy:deploy-file -Dfile=hy.common.redis-sources.jar -Dclassifier=sources -DpomFile=./src/META-INF/maven/org/hy/common/redis/pom.xml -DrepositoryId=thirdparty -Durl=http://218.21.3.19:1481/nexus/content/repositories/thirdparty
