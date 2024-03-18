package org.hy.common.redis.junit;

import java.util.ArrayList;
import java.util.List;

import org.hy.common.Date;
import org.hy.common.Help;
import org.hy.common.StringHelp;
import org.hy.common.redis.IRedis;
import org.hy.common.redis.cluster.RedisClusterConfig;
import org.hy.common.xml.XJava;
import org.hy.common.xml.annotation.XType;
import org.hy.common.xml.annotation.Xjava;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import io.lettuce.core.RedisURI;
import io.lettuce.core.cluster.ClusterClientOptions;
import io.lettuce.core.cluster.RedisClusterClient;
import io.lettuce.core.cluster.api.sync.RedisAdvancedClusterCommands;





/**
 * 测试单元：Redis的Lettuce客户端实现
 *
 * @author      ZhengWei(HY)
 * @createDate  2024-03-14
 * @version     v1.0
 */
@Xjava(value=XType.XML)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class JU_RedisLettuce
{
    private static boolean $isInit = false;
    
    
    
    public JU_RedisLettuce() throws Exception
    {
        if ( !$isInit )
        {
            $isInit = true;
            XJava.parserAnnotation(this.getClass().getName());
        }
    }
    
    
    
    @Test
    public void test_XJava_GetTables()
    {
        IRedis v_RedisOpt = (IRedis) XJava.getObject("RedisOperation");
        String v_PK       = "";
        
        for (int x=1; x<=9; x++)
        {
            v_PK = StringHelp.getUUID();
            v_RedisOpt.insert("库" ,"表v1." + x ,v_PK ,"createTime" ,new Date().getFull());
        }
        
        // 用两种方法查询全库中的表信息
        Help.print(v_RedisOpt.getRows("库"));
        Help.print(v_RedisOpt.getTables("库"));
        
        System.out.println("库名存在？" + v_RedisOpt.isExists("库"));
        System.out.println("表名存在？" + v_RedisOpt.isExists("库" ,"表v1.2"));
        System.out.println("主键存在？" + v_RedisOpt.isExists("库" ,"表v1.9" ,v_PK));
        System.out.println("字段存在？" + v_RedisOpt.isExists("库" ,"表v1.9" ,v_PK ,"createTime"));
        
        System.out.println("库创建时间：" + v_RedisOpt.getCreateTime("库"));
        System.out.println("表创建时间：" + v_RedisOpt.getCreateTime("库" ,"表v1.1"));
        System.out.println("行创建时间：" + v_RedisOpt.getCreateTime("库" ,"表v1.9" ,v_PK));
        
        v_RedisOpt.dropDatabase("库");
    }
    
    
    
    @Test
    public void test_XJava_GetAll()
    {
        IRedis v_RedisOpt = (IRedis) XJava.getObject("RedisOperation");
        
        System.out.println("开始写入：" + Date.getNowTime().getFullMilli());
        for (int x=1; x<=10; x++)
        {
            String v_ID  = "书名ID" + x;
            Book v_Book = new Book(v_ID ,x * 1D ,new Date());
            
            v_RedisOpt.insert("图书馆" ,"科学类" ,v_ID ,v_Book);
        }
        System.out.println("写入完成：" + Date.getNowTime().getFullMilli());
        
        Help.print(v_RedisOpt.getRows("图书馆" ,"科学类"));
        
        System.out.println("读取完成：" + Date.getNowTime().getFullMilli());
        
        for (int x=1; x<=10; x++)
        {
            String v_ID  = "书名ID" + x;
            v_RedisOpt.delete("图书馆" ,"科学类" ,v_ID);
        }
        System.out.println("删除完成：" + Date.getNowTime().getFullMilli());
    }
    
    
    
    @Test
    public void test_XJava_Insert_Serializable_Object()
    {
        IRedis v_RedisOpt = (IRedis) XJava.getObject("RedisOperation");
        String v_ID       = "国棉二厂";
        School v_Row      = new School(v_ID ,32D ,new Date("1990-09-01"));
        
        v_RedisOpt.insert("学校" ,v_ID ,v_Row.getName() ,v_Row);
        Help.print(v_RedisOpt.getRow(v_ID));
        
        School v_New = v_RedisOpt.getRow(v_ID ,School.class);
        System.out.println(v_New);
    }
    
    
    
    @Test
    public void test_XJava_Insert_Normal_Object()
    {
        IRedis v_RedisOpt = (IRedis) XJava.getObject("RedisOperation");
        Book   v_Row      = new Book("语文" ,10D ,new Date());
        
        v_RedisOpt.insert("图书馆" ,"书" ,v_Row.getName() ,v_Row);
        Help.print(v_RedisOpt.getRow("语文"));
        
        Book v_New = v_RedisOpt.getRow("语文" ,Book.class);
        System.out.println(v_New);
    }
    
    
    
    @Test
    public void test_XJava_Get()
    {
        IRedis v_RedisOpt = (IRedis) XJava.getObject("RedisOperation");
        
        Help.print(v_RedisOpt.getRow("主键"));
    }
    
    
    
    @Test
    public void test_XJava_Insert()
    {
        IRedis v_RedisOpt = (IRedis) XJava.getObject("RedisOperation");
        
        Long v_Ret = v_RedisOpt.insert("库名" ,"表名" ,"主键" ,"字段名" ,"字段值");
        System.out.println("插入结果：" + v_Ret);
        
        v_Ret = v_RedisOpt.update("库名" ,"表名" ,"主键" ,"字段名" ,"字段值1");
        System.out.println("更新结果：" + v_Ret);
        
        v_Ret = v_RedisOpt.update("库名" ,"表名" ,"主键" ,"字段名" ,"字段值2");
        System.out.println("更新结果：" + v_Ret);
    }
    
    
    
    @Test
    public void test_Connect_XJava()
    {
        RedisClusterConfig v_RedisClusterConfig = (RedisClusterConfig) XJava.getObject("RedisClusterConfig");
        RedisClusterClient v_RedisCluster       = v_RedisClusterConfig.toLettuce();


        // 创建连接到 Redis 集群的连接
        try
        {
            RedisAdvancedClusterCommands<String ,String> v_RedisClusterCmds = v_RedisCluster.connect().sync();
            String v_K2 = v_RedisClusterCmds.get("k2");
            System.out.println("K2 = " + v_K2);
            System.out.println("Successfully connected to the Redis cluster");
        }
        catch (Exception e)
        {
            System.err.println("Failed to connect to the Redis cluster: " + e.getMessage());
        }
        finally
        {
            // 关闭集群客户端连接
            v_RedisCluster.shutdown();
        }
    }
    
    
    
    @Test
    public void test_Connect()
    {
        char []   v_RedisPasswd = "密码".toCharArray();
        String [] v_RedisIPPort = {"10.1.85.23:18501"
                                  ,"10.1.85.23:18502"
                                  ,"10.1.85.22:18503"
                                  ,"10.1.85.22:18504"
                                  ,"10.1.85.21:18505"
                                  ,"10.1.85.21:18506"
        };
        
        List<RedisURI> v_Redis = new ArrayList<RedisURI>();
        
        for (String v_IPPort : v_RedisIPPort)
        {
            String [] v_IPPortArr = v_IPPort.split(":");
            v_Redis.add(RedisURI.builder()
                                .withHost(v_IPPortArr[0])
                                .withPort(Integer.parseInt(v_IPPortArr[1]))
                                .withPassword(v_RedisPasswd)
                                .build());
        }
        
        RedisClusterClient v_RedisCluster = RedisClusterClient.create(v_Redis);

        // 设置集群客户端选项
        ClusterClientOptions v_ClusterClientOptions = ClusterClientOptions.builder().autoReconnect(true).build();
        v_RedisCluster.setOptions(v_ClusterClientOptions);

        // 创建连接到 Redis 集群的连接
        try
        {
            RedisAdvancedClusterCommands<String ,String> v_RedisClusterCmds = v_RedisCluster.connect().sync();
            String v_K2 = v_RedisClusterCmds.get("k2");
            System.out.println("K2 = " + v_K2);
            System.out.println("Successfully connected to the Redis cluster");
        }
        catch (Exception e)
        {
            System.err.println("Failed to connect to the Redis cluster: " + e.getMessage());
        }
        finally
        {
            // 关闭集群客户端连接
            v_RedisCluster.shutdown();
        }
    }
}
