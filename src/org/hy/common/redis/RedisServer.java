package org.hy.common.redis;

import java.util.ArrayList;
import java.util.List;

import org.hy.common.Date;
import org.hy.common.JavaHelp;
import org.hy.common.Return;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.ShardedJedis;





/**
 * Redis数据库中的Server服务对象的相关操作类
 * 
 * @author ZhengWei(HY)
 * @create 2014-10-17
 */
public class RedisServer
{
    private Redis redis;
    
    
    
    public RedisServer(Redis i_Redis)
    {
        if ( i_Redis == null )
        {
            throw new NullPointerException("Redis is null.");
        }
        
        this.redis = i_Redis;
    }
    
    
    
    /**
     * Redis数据库在【多套集群分布式】情况下执行bgsave保存所有分布式数据库
     * 
     * 原始命令为：BGSAVE
     * 
     * 在后台异步(Asynchronously)保存当前数据库的数据到磁盘。
     * 
     * BGSAVE 命令执行之后立即返回 OK ，然后 Redis fork 出一个新子进程，
     * 原来的 Redis 进程(父进程)继续处理客户端请求，而子进程则负责将数据保存到磁盘，然后退出。
     */
    public void saveBackground()
    {
        this.core_saveBackground(null);
    }
    
    
    
    /**
     * Redis数据库在【多套集群分布式】情况下执行某一套集群数据库，执行保存bgsave命令
     * 
     * 原始命令为：BGSAVE
     * 
     * 在后台异步(Asynchronously)保存当前数据库的数据到磁盘。
     * 
     * BGSAVE 命令执行之后立即返回 OK ，然后 Redis fork 出一个新子进程，
     * 原来的 Redis 进程(父进程)继续处理客户端请求，而子进程则负责将数据保存到磁盘，然后退出。
     * 
     * @param i_ShardName  分布式集群中集群的名称。为空时，对所有集群数据库执行bgsave命令
     */
    public void saveBackground(String i_ShardName)
    {
        this.core_saveBackground(i_ShardName);
    }
    
    
    
    /**
     * Redis数据库在【多套集群分布式】情况下执行bgsave保存所有分布式数据库
     * 
     * 原始命令为：BGSAVE
     * 
     * 在后台异步(Asynchronously)保存当前数据库的数据到磁盘。
     * 
     * BGSAVE 命令执行之后立即返回 OK ，然后 Redis fork 出一个新子进程，
     * 原来的 Redis 进程(父进程)继续处理客户端请求，而子进程则负责将数据保存到磁盘，然后退出。
     * 
     * @param i_ShardName  分布式集群中集群的名称。为空时，对所有集群数据库执行bgsave命令
     */
    private void core_saveBackground(String i_ShardName)
    {
        ShardedJedis v_ShardedJedis = null;
        
        try
        {
            v_ShardedJedis = this.redis.getWriter();
            
            if ( JavaHelp.isNull(i_ShardName) )
            {
                for (Jedis v_Jedis : v_ShardedJedis.getAllShards())
                {
                    v_Jedis.bgsave();
                }
            }
            else
            {
                v_ShardedJedis.getShard(i_ShardName).bgsave();
            }
            
            this.redis.returnResource(v_ShardedJedis);
        }
        catch (Exception exce)
        {
            this.redis.returnResource(v_ShardedJedis ,exce);
        }
    }
    
    
    
    /**
     * Redis数据库在【多套集群分布式】情况下执行save保存所有分布式数据库
     * 
     * 原始命令为：SAVE
     * 
     * 命令执行一个同步保存操作，
     * 将当前 Redis 实例的所有数据快照(snapshot)以 RDB 文件的形式保存到硬盘。
     * 
     * 一般来说，在生产环境很少执行 SAVE 操作，因为它会阻塞所有客户端，
     * 保存数据库的任务通常由 BGSAVE 命令异步地执行。
     * 然而，如果负责保存数据的后台子进程不幸出现问题时， 
     * SAVE 可以作为保存数据的最后手段来使用。
     */
    public void save()
    {
        this.core_save(null);
    }
    
    
    
    /**
     * Redis数据库在【多套集群分布式】情况下执行某一套集群数据库，执行保存save命令
     * 
     * 原始命令为：SAVE
     * 
     * 命令执行一个同步保存操作，
     * 将当前 Redis 实例的所有数据快照(snapshot)以 RDB 文件的形式保存到硬盘。
     * 
     * 一般来说，在生产环境很少执行 SAVE 操作，因为它会阻塞所有客户端，
     * 保存数据库的任务通常由 BGSAVE 命令异步地执行。
     * 然而，如果负责保存数据的后台子进程不幸出现问题时， 
     * SAVE 可以作为保存数据的最后手段来使用。
     * 
     * @param i_ShardName  分布式集群中集群的名称。为空时，对所有集群数据库执行save命令
     */
    public void save(String i_ShardName)
    {
        this.core_save(i_ShardName);
    }
    
    
    
    /**
     * Redis数据库在【多套集群分布式】情况下执行save保存所有分布式数据库
     * 
     * 原始命令为：SAVE
     * 
     * 命令执行一个同步保存操作，
     * 将当前 Redis 实例的所有数据快照(snapshot)以 RDB 文件的形式保存到硬盘。
     * 
     * 一般来说，在生产环境很少执行 SAVE 操作，因为它会阻塞所有客户端，
     * 保存数据库的任务通常由 BGSAVE 命令异步地执行。
     * 然而，如果负责保存数据的后台子进程不幸出现问题时， 
     * SAVE 可以作为保存数据的最后手段来使用。
     * 
     * @param i_ShardName  分布式集群中集群的名称。为空时，对所有集群数据库执行save命令
     */
    private void core_save(String i_ShardName)
    {
        ShardedJedis v_ShardedJedis = null;
        
        try
        {
            v_ShardedJedis = this.redis.getWriter();
            
            if ( JavaHelp.isNull(i_ShardName) )
            {
                for (Jedis v_Jedis : v_ShardedJedis.getAllShards())
                {
                    v_Jedis.save();
                }
            }
            else
            {
                v_ShardedJedis.getShard(i_ShardName).save();
            }
            
            this.redis.returnResource(v_ShardedJedis);
        }
        catch (Exception exce)
        {
            this.redis.returnResource(v_ShardedJedis ,exce);
        }
    }
    
    
    
    /**
     * Redis数据库在【多套集群分布式】情况下获取所有分布式数据库的合计Keys数量
     * 
     * 原始命令为：DBSIZE
     * 
     * 返回当前数据库的 key 的数量。
     * 
     * @return             异常返回 null
     */
    public Long dbSize()
    {
        return this.core_dbSize(null);
    }
    
    
    
    /**
     * Redis数据库在【多套集群分布式】情况下获取某一套集群数据库，的Keys数量
     * 
     * 原始命令为：DBSIZE
     * 
     * @param i_ShardName  分布式集群中集群的名称。为空时，获取所有集群数据库的Keys数量
     * @return             异常返回 null
     */
    public Long dbSize(String i_ShardName)
    {
        return this.core_dbSize(i_ShardName);
    }
    
    
    
    /**
     * Redis数据库在【多套集群分布式】情况下获取某一套集群数据库，的Keys数量
     * 
     * 原始命令为：DBSIZE
     * 
     * @param i_ShardName  分布式集群中集群的名称。为空时，获取所有集群数据库的Keys数量
     * @return             异常返回 null
     */
    private Long core_dbSize(String i_ShardName)
    {
        ShardedJedis v_ShardedJedis = null;
        Long         v_Ret          = 0L;
        
        try
        {
            v_ShardedJedis = this.redis.getWriter();
            
            if ( JavaHelp.isNull(i_ShardName) )
            {
                for (Jedis v_Jedis : v_ShardedJedis.getAllShards())
                {
                    v_Ret += v_Jedis.dbSize();
                }
            }
            else
            {
                v_Ret = v_ShardedJedis.getShard(i_ShardName).dbSize();
            }
            
            this.redis.returnResource(v_ShardedJedis);
        }
        catch (Exception exce)
        {
            v_Ret = null;
            this.redis.returnResource(v_ShardedJedis ,exce);
        }
        
        return v_Ret;
    }
    
    
    
    /**
     * Redis数据库在【多套集群分布式】情况下获取某一套集群数据库，最近一次 Redis 成功将数据保存到磁盘上的时间
     * 
     * 原始命令为：LASTSAVE
     * 
     * @param i_ShardName  分布式集群中集群的名称
     * @return             异常返回 null
     */
    public Date saveLastTime(String i_ShardName)
    {
        ShardedJedis v_ShardedJedis = null;
        Date         v_Ret          = null;
        
        try
        {
            v_ShardedJedis = this.redis.getReader();
            v_Ret = new Date(v_ShardedJedis.getShard(i_ShardName).lastsave());
            
            this.redis.returnResource(v_ShardedJedis);
        }
        catch (Exception exce)
        {
            v_Ret = null;
            this.redis.returnResource(v_ShardedJedis ,exce);
        }
        
        return v_Ret;
    }
    
    
    
    /**
     * Redis数据库在【多套集群分布式】情况对所有分布式集群数据库，执行flushDB命令
     * 
     * 原始命令为：FLUSHDB
     * 
     * 清空当前数据库中的所有 key。
     */
    public void flushDB()
    {
        this.core_flushDB(null);
    }
    
    
    
    /**
     * Redis数据库在【多套集群分布式】情况对某一套集群(或所有分布式集群)数据库，执行flushDB命令
     * 
     * 原始命令为：FLUSHDB
     * 
     * 清空当前数据库中的所有 key。
     * 
     * @param i_ShardName  分布式集群中集群的名称。为空时，对所有集群数据库的执行flushDB命令
     */
    public void flushDB(String i_ShardName)
    {
        this.core_flushDB(i_ShardName);
    }
    
    
    
    /**
     * Redis数据库在【多套集群分布式】情况对某一套集群(或所有分布式集群)数据库，执行flushDB命令
     * 
     * 原始命令为：FLUSHDB
     * 
     * 清空当前数据库中的所有 key。
     * 
     * @param i_ShardName  分布式集群中集群的名称。为空时，对所有集群数据库的执行flushDB命令
     */
    private void core_flushDB(String i_ShardName)
    {
        ShardedJedis v_ShardedJedis = null;
        
        try
        {
            v_ShardedJedis = this.redis.getWriter();
            
            if ( JavaHelp.isNull(i_ShardName) )
            {
                for (Jedis v_Jedis : v_ShardedJedis.getAllShards())
                {
                    v_Jedis.flushDB();
                }
            }
            else
            {
                v_ShardedJedis.getShard(i_ShardName).flushDB();
            }
            
            this.redis.returnResource(v_ShardedJedis);
        }
        catch (Exception exce)
        {
            this.redis.returnResource(v_ShardedJedis ,exce);
        }
    }
    
    
    
    /**
     * Redis数据库在【多套集群分布式】情况对所有分布式集群数据库，执行flushAll命令
     * 
     * 原始命令为：FLUSHALL
     * 
     * 清空整个 Redis 服务器的数据(删除所有数据库的所有 key )。
     */
    public void flushDBAll()
    {
        this.core_flushDBAll(null);
    }
    
    
    
    /**
     * Redis数据库在【多套集群分布式】情况对某一套集群(或所有分布式集群)数据库，执行flushAll命令
     * 
     * 原始命令为：FLUSHALL
     * 
     * 清空整个 Redis 服务器的数据(删除所有数据库的所有 key )。
     * 
     * @param i_ShardName  分布式集群中集群的名称。为空时，对所有集群数据库的执行flushAll命令
     */
    public void flushDBAll(String i_ShardName)
    {
        this.core_flushDBAll(i_ShardName);
    }
    
    
    
    /**
     * Redis数据库在【多套集群分布式】情况对某一套集群(或所有分布式集群)数据库，执行flushAll命令
     * 
     * 原始命令为：FLUSHALL
     * 
     * 清空整个 Redis 服务器的数据(删除所有数据库的所有 key )。
     * 
     * @param i_ShardName  分布式集群中集群的名称。为空时，对所有集群数据库的执行flushAll命令
     */
    private void core_flushDBAll(String i_ShardName)
    {
        ShardedJedis v_ShardedJedis = null;
        
        try
        {
            v_ShardedJedis = this.redis.getWriter();
            
            if ( JavaHelp.isNull(i_ShardName) )
            {
                for (Jedis v_Jedis : v_ShardedJedis.getAllShards())
                {
                    v_Jedis.flushAll();
                }
            }
            else
            {
                v_ShardedJedis.getShard(i_ShardName).flushAll();
            }
            
            this.redis.returnResource(v_ShardedJedis);
        }
        catch (Exception exce)
        {
            this.redis.returnResource(v_ShardedJedis ,exce);
        }
    }
    
    
    
    /**
     * Redis数据库在【多套集群分布式】情况对所有分布式集群数据库，执行ping命令
     * 
     * 原始命令为：PING
     * 
     * 使用客户端向 Redis 服务器发送一个 PING ，如果服务器运作正常的话，会返回一个 PONG 。
     * 
     * @return             当某些集群异常时，Return.paramObj 记录出现异常的集群名称
     */
    public Return<List<String>> ping()
    {
        return this.core_ping(null);
    }
    
    
    
    /**
     * Redis数据库在【多套集群分布式】情况对某一套集群(或所有分布式集群)数据库，执行ping命令
     * 
     * 原始命令为：PING
     * 
     * 使用客户端向 Redis 服务器发送一个 PING ，如果服务器运作正常的话，会返回一个 PONG 。
     * 
     * @param i_ShardName  分布式集群中集群的名称。为空时，对所有集群数据库的执行ping命令
     * @return             当某些集群异常时，Return.paramObj 记录出现异常的集群名称
     */
    public Return<List<String>> ping(String i_ShardName)
    {
        return this.core_ping(i_ShardName);
    }
    
    
    
    /**
     * Redis数据库在【多套集群分布式】情况对某一套集群(或所有分布式集群)数据库，执行ping命令
     * 
     * 原始命令为：PING
     * 
     * 使用客户端向 Redis 服务器发送一个 PING ，如果服务器运作正常的话，会返回一个 PONG 。
     * 
     * @param i_ShardName  分布式集群中集群的名称。为空时，对所有集群数据库的执行ping命令
     * @return             当某些集群异常时，Return.paramObj 记录出现异常的集群名称
     */
    private Return<List<String>> core_ping(String i_ShardName)
    {
        ShardedJedis         v_ShardedJedis = null;
        Return<List<String>> v_Ret          = new Return<List<String>>(true);
        
        try
        {
            v_ShardedJedis = this.redis.getWriter();
            
            if ( JavaHelp.isNull(i_ShardName) )
            {
                v_Ret.paramObj = new ArrayList<String>(this.redis.getShardSize());
                
                List<String> v_ShardNames = this.redis.getShardNames();
                
                if ( JavaHelp.isNull(v_ShardNames) )
                {
                    throw new NullPointerException("ShardInfos is null.");
                }
                
                for (int v_ShardIndex=v_ShardNames.size() - 1; v_ShardIndex>=0; v_ShardIndex--)
                {
                    String v_IsPONG = v_ShardedJedis.getShard(v_ShardNames.get(v_ShardIndex)).ping();
                    
                    if ( JavaHelp.isNull(v_IsPONG) || !"PONG".equals(v_IsPONG) )
                    {
                        v_Ret.set(false);
                        v_Ret.paramObj.add(v_ShardNames.get(v_ShardIndex));
                    }
                }
            }
            else
            {
                String v_IsPONG = v_ShardedJedis.getShard(i_ShardName).ping();
                
                if ( JavaHelp.isNull(v_IsPONG) || !"PONG".equals(v_IsPONG) )
                {
                    v_Ret.set(false);
                    v_Ret.paramObj = new ArrayList<String>(1);
                    v_Ret.paramObj.add(i_ShardName);
                }
            }
            
            this.redis.returnResource(v_ShardedJedis);
        }
        catch (Exception exce)
        {
            v_Ret.set(false);
            this.redis.returnResource(v_ShardedJedis ,exce);
        }
        
        return v_Ret;
    }
}
