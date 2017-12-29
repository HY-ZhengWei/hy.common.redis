#!/bin/sh

cd bin


rm -R ./org/hy/common/redis/junit


jar cvfm hy.common.redis.jar MANIFEST.MF META-INF org

cp hy.common.redis.jar ..
rm hy.common.redis.jar
cd ..

