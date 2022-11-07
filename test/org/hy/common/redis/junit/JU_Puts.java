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
 * ����Redis���ݿ��ڡ����׼�Ⱥ�ֲ�ʽ�������ִ��set��������
 * 
 * �ص����ڲ������񡢶༯Ⱥ�쳣���ֲ�ʽ�쳣��
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
        // ���÷ֲ�ʽ��Ⱥ����
        List<JedisShardInfo> v_JSIs = new ArrayList<JedisShardInfo>();
        
        v_JSIs.add(new JedisShardInfo("192.168.105.105" ,6379));  // ��Ⱥ1�е���������
        // v_JSIs.add(new JedisShardInfo("192.168.105.109" ,6379));  // ��Ⱥ2�е���������
        
        // ��������
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
        
        // Ϊ�˲��ԣ�����д���״γɹ��󣬵ڶ���д֮ǰ����Ϊ���һRedis��Ⱥ�����쳣
        this.redis.getRHash().put(v_RDatas);
        
        
        System.out.println("-- д����ɡ��̵߳ȴ��С�����");
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
