package org.hy.common.redis.junit;

import java.util.ArrayList;
import java.util.List;

import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import redis.clients.jedis.JedisShardInfo;

import org.hy.common.redis.Redis;





/**
 * 测试Redis数据库在【多套集群分布式】情况下执行bgsave命令的情况
 * 
 * @author ZhengWei(HY)
 * @create 2014-10-17
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING) 
public class JU_SaveBackground
{
    private Redis redis;
    
    
    
    public JU_SaveBackground()
    {
        // 配置分布式集群访问
        List<JedisShardInfo> v_JSIs = new ArrayList<JedisShardInfo>();
        
        v_JSIs.add(new JedisShardInfo("192.168.105.105" ,6379));  // 集群1中的主服务器
        v_JSIs.add(new JedisShardInfo("192.168.105.109" ,6379));  // 集群2中的主服务器
        
        redis = new Redis(v_JSIs);
    }
    
    
    
    @Test
    public void test_001()
    {
        redis.getRServer().saveBackground();
        redis.getRServer().saveBackground(redis.getShardNames().get(0));
    }
    
    
    
    @Test
    public void test_002()
    {
        System.out.println(redis.getRServer().dbSize());
    }
    
}
