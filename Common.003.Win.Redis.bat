

cd .\bin


rd /s/q .\org\hy\common\redis\junit


jar cvfm hy.common.redis.jar MANIFEST.MF META-INF org

copy hy.common.redis.jar ..
del /q hy.common.redis.jar
cd ..





cd .\src
jar cvfm hy.common.redis-sources.jar MANIFEST.MF META-INF org 
copy hy.common.redis-sources.jar ..
del /q hy.common.redis-sources.jar
cd ..
