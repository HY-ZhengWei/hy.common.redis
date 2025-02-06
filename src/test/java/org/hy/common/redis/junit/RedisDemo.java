package org.hy.common.redis.junit;

import java.util.ArrayList;
import java.util.List;

import org.hy.common.Date;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.JedisShardInfo;
import redis.clients.jedis.ShardedJedis;
import redis.clients.jedis.ShardedJedisPool;





public class RedisDemo
{

    @SuppressWarnings({"resource" ,"deprecation"})
    public static void main(String [] args)
    {
        // 配置单机访问
        @SuppressWarnings("resource")
        Jedis v_Jedis = new Jedis("127.0.0.1");
        v_Jedis.set("test" ,new Date().getFull());
        System.out.println(v_Jedis.get("test"));
        
        
        
        
        
        // 配置集群访问
        // 这里只配置了一台主服务器，因为从机配置的只读模式 slave-read-only yes
        JedisShardInfo v_JedisPool_01 = new JedisShardInfo("127.0.0.1" , 6379);
        List<JedisShardInfo> v_JedisPoolList = new ArrayList<JedisShardInfo>();
        
        v_JedisPoolList.add(v_JedisPool_01);
        
        JedisPoolConfig v_JedisPoolConfig = new JedisPoolConfig();
        v_JedisPoolConfig.setMaxTotal(1024);
        v_JedisPoolConfig.setMaxIdle(10);
        v_JedisPoolConfig.setMinIdle(1);
        
        ShardedJedisPool v_ShardedJedisPool = new ShardedJedisPool(v_JedisPoolConfig ,v_JedisPoolList);
        
        
        ShardedJedis v_ShardedJedis = v_ShardedJedisPool.getResource();
        
        v_ShardedJedis.set("test" ,"bar");
        System.out.println(v_ShardedJedis.get("test"));
        
        
        // 返还到连接池。正常情况下使用此方法
        v_ShardedJedisPool.returnResource(v_ShardedJedis);
        
        // 释放Redis对象。
        // 在程序出错时，必须调用returnBrokenResource返还给pool，
        // 否则下次通过getResource得到的instance的缓冲区可能还存在数据，出现问题
        v_ShardedJedisPool.returnBrokenResource(v_ShardedJedis);
        
        v_ShardedJedisPool.destroy();
    }
    
}
