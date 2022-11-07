package org.hy.common.redis.junit;

import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import org.hy.common.redis.Redis;





@FixMethodOrder(MethodSorters.NAME_ASCENDING) 
public class JU_Redis
{
    private Redis redis;
    
    
    
    public JU_Redis()
    {
        // 配置集群访问
        // 这里只配置了一台主服务器，因为从机配置的只读模式 slave-read-only yes
        redis = new Redis("127.0.0.1" ,6379);
    }
    
    
    @Test
    public void test_001_createTable()
    {
        String v_TableName = "T_UserInfo";
        
        if ( redis.isExistsTable(v_TableName) )
        {
            redis.dropTable(v_TableName);
        }

        redis.createTable(v_TableName);
        redis.insert(     v_TableName ,"Row001" ,"Name" ,"ZhengWei");
        redis.insert(     v_TableName ,"Row001" ,"Sex"  ,"1");
        
        redis.insert(     v_TableName ,"Row002" ,"Name" ,"WMJ");
        redis.insert(     v_TableName ,"Row002" ,"Sex"  ,"0");
        
        
        redis.showTableDatas(v_TableName);
    }
    
    
    
    // @Test
    public void test_999_deleteAll()
    {
        String v_TableName = "T_UserInfo";
        
        redis.deleteAll(v_TableName);
    }
    
}
