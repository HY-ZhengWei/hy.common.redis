package org.hy.common.redis.junit;

import java.util.ArrayList;
import java.util.List;

import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import redis.clients.jedis.JedisShardInfo;

import org.hy.common.Date;
import org.hy.common.redis.RData;
import org.hy.common.redis.Redis;





/**
 * ����Redis���ݿ������
 * 
 * @author ZhengWei(HY)
 * @create 2014-10-16
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING) 
public class JU_Transaction
{
    private Redis redis;
    
    
    
    public JU_Transaction()
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
        List<RData> v_RDatas = new ArrayList<RData>();
        
        v_RDatas.add(new RData("1" ,"1"));
        v_RDatas.add(new RData("2" ,"2"));
        
        this.redis.getRString().put("QQ" ,Date.getNowTime().getFullMilli());
        
        // this.redis.getRString().puts(v_RDatas);
    }
    
}
