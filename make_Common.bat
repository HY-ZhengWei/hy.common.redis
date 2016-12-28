cd bin

rd /s /q .\com\hy\common\redis\junit

jar cvfm hy.common.redis.jar MANIFEST.MF com
copy hy.common.redis.jar ..
del /q hy.common.redis.jar