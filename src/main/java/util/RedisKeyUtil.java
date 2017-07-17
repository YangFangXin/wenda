package util;

public class RedisKeyUtil {
	private static final String SPLIT = ":";
	private static final String BIZ_LIKE = "LIKE";
	private static final String BIZ_DISLIKE = "DISLIKE";
	private static final String ASYNC_QUEUE = "ASYNC_QUEUE";
	
	/*
	 * 返回该实体对应的key
	 */
	public static String getLikeKey(int entityId,int entityType)
	{
		return BIZ_LIKE+SPLIT+String.valueOf(entityType)+SPLIT+String.valueOf(entityId);
	}
	
	public static String getDislikeKey(int entityId,int entityType)
	{
		return BIZ_DISLIKE+SPLIT+String.valueOf(entityType)+SPLIT+String.valueOf(entityId);
	}
	
	public static String getAsyncQueueKey(int priority)
	{
		return ASYNC_QUEUE+SPLIT+priority;
	}
}