#!/bin/sh

mvn deploy:deploy-file -Dfile=hy.common.redis.jar                              -DpomFile=./src/META-INF/maven/org/hy/common/redis/pom.xml -DrepositoryId=thirdparty -Durl=http://HY-ZhengWei:1481/repository/thirdparty
mvn deploy:deploy-file -Dfile=hy.common.redis-sources.jar -Dclassifier=sources -DpomFile=./src/META-INF/maven/org/hy/common/redis/pom.xml -DrepositoryId=thirdparty -Durl=http://HY-ZhengWei:1481/repository/thirdparty
