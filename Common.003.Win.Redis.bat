cd bin


rd /s/q .\org\hy\common\redis\junit


jar cvfm hy.common.redis.jar MANIFEST.MF META-INF org

copy hy.common.redis.jar ..
del /q hy.common.redis.jar
cd ..

