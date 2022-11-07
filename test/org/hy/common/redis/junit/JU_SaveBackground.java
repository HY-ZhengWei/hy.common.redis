package org.hy.common.redis.junit;

import java.util.ArrayList;
import java.util.List;

import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import redis.clients.jedis.JedisShardInfo;

import org.hy.common.redis.Redis;





/**
 * ����Redis���ݿ��ڡ����׼�Ⱥ�ֲ�ʽ�������ִ��bgsave��������
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
        // ���÷ֲ�ʽ��Ⱥ����
        List<JedisShardInfo> v_JSIs = new ArrayList<JedisShardInfo>();
        
        v_JSIs.add(new JedisShardInfo("192.168.105.105" ,6379));  // ��Ⱥ1�е���������
        v_JSIs.add(new JedisShardInfo("192.168.105.109" ,6379));  // ��Ⱥ2�е���������
        
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
