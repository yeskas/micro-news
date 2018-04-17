#!/usr/bin/env python
import redis


if __name__ == '__main__':
	redis_client = redis.StrictRedis(host='localhost', port=6379, db=0)

	for news_key in redis_client.scan_iter("news:by_*"):
		redis_client.delete(news_key)
		print 'Deleted %s:' % news_key
