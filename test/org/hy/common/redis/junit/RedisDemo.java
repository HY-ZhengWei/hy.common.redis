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

    public static void main(String [] args)
    {
        // ���õ�������
        @SuppressWarnings("resource")
        Jedis v_Jedis = new Jedis("127.0.0.1");
        v_Jedis.set("test" ,new Date().getFull());
        System.out.println(v_Jedis.get("test"));
        
        
        
        
        
        // ���ü�Ⱥ����
        // ����ֻ������һ̨������������Ϊ�ӻ����õ�ֻ��ģʽ slave-read-only yes
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
        
        
        // ���������ӳء����������ʹ�ô˷���
        v_ShardedJedisPool.returnResource(v_ShardedJedis);
        
        // �ͷ�Redis����
        // �ڳ������ʱ���������returnBrokenResource������pool��
        // �����´�ͨ��getResource�õ���instance�Ļ��������ܻ��������ݣ���������
        v_ShardedJedisPool.returnBrokenResource(v_ShardedJedis);
        
        v_ShardedJedisPool.destroy();
    }
    
}
