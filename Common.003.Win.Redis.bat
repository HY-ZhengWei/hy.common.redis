

del /Q hy.common.redis.jar
del /Q hy.common.redis-sources.jar


call mvn clean package
cd .\target\classes

rd /s/q .\org\hy\common\redis\junit


jar cvfm hy.common.redis.jar META-INF/MANIFEST.MF META-INF org

copy hy.common.redis.jar ..\..
del /q hy.common.redis.jar
cd ..\..





cd .\src\main\java
xcopy /S ..\resources\* .
jar cvfm hy.common.redis-sources.jar META-INF\MANIFEST.MF META-INF org 
copy hy.common.redis-sources.jar ..\..\..
del /Q hy.common.redis-sources.jar
rd /s/q META-INF
cd ..\..\..

pause