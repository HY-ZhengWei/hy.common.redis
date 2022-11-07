package org.hy.common.redis.junit;

import java.util.ArrayList;
import java.util.List;

import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import redis.clients.jedis.JedisShardInfo;

import org.hy.common.Date;
import org.hy.common.StringHelp;
import org.hy.common.redis.RData;
import org.hy.common.redis.Redis;
import org.hy.common.redis.Redis.RunMode;





/**
 * 测试Redis数据库在【多套集群分布式】情况下执行set命令的情况
 * 
 * 重点在于测试事务、多集群异常，分布式异常等
 * 
 * @author ZhengWei(HY)
 * @create 2014-10-18
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING) 
public class JU_Puts
{
    private Redis redis;
    
    
    
    public JU_Puts()
    {
        // 配置分布式集群访问
        List<JedisShardInfo> v_JSIs = new ArrayList<JedisShardInfo>();
        
        v_JSIs.add(new JedisShardInfo("192.168.105.105" ,6379));  // 集群1中的主服务器
        // v_JSIs.add(new JedisShardInfo("192.168.105.109" ,6379));  // 集群2中的主服务器
        
        // 设置密码
        for (JedisShardInfo v_Shard : v_JSIs)
        {
            v_Shard.setPassword("redis");
        }
        
        redis = new Redis(v_JSIs);
        redis.setRunMode(RunMode.$Backup);
    }
    
    
    
    @Test
    public void test_001()
    {
        List<RData> v_RDatas = new ArrayList<RData>();
        
        for (int x=0; x<2; x++)
        {
            for (int i=0; i<10; i++)
            {
                v_RDatas.add(new RData("JU_Puts_" + x ,"Field_" + StringHelp.lpad(i ,2 ,"0") ,Date.getNowTime().getFullMilli()));
            }
        }
        
        // 为了测试，请在写入首次成功后，第二次写之前，人为造成一Redis集群服务异常
        this.redis.getRHash().put(v_RDatas);
        
        
        System.out.println("-- 写入完成。线程等待中。。。");
        try
        {
            Thread.sleep(10 * 60 * 1000);
        }
        catch (Exception exce)
        {
            exce.printStackTrace();
        }
    }
    
}
