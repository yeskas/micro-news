import copy
import datetime
import json
import pdb
from pprint import pprint
import re

import pika
from pyquery import PyQuery
import redis

from config import rabbitmq_settings, amqp_addr


# Fetch the list of latest news based on the source-specific configs
def download_article_links(source):
	url = source['parsing_data']['list']['url']
	sel = source['parsing_data']['list']['selectors']

	# 'pq' acts like jquery's '$'
	pq = PyQuery(url=url)

	link_elems = pq(sel['article_link'])
	links = []
	for link_elem in link_elems:
		link = link_elem.attrib['href']

		# relative url
		if link.startswith('/'):
			link = source['link'] + link

		links.append(link)

	return links


# Download and parse single article, return None if cant parse
def download_article(source, link):
	# 'pq' acts like jquery's '$'
	pq = PyQuery(url=link)
	sel = source['parsing_data']['article']['selectors']

	title = pq(sel['title']).text()
	body = pq(sel['body']).text()

	image = ''

	# try all known ways to extract image
	image_elems = pq(sel['image'])
	for image_elem in image_elems:
		if image_elem.tag == 'img':
			# regular img tag
			image = image_elem.get('src') or image_elem.get('data-src')

		else:
			# part of background-image style
			style = image_elem.get('style', '')
			match = re.search('background-image: url(.*)', style, re.IGNORECASE)
			if match:
				image = match.group(1)[2:-2]

			# part of a video player poster
			else:
				image = image_elem.get('data-poster', '')

		if image != '':
			break

	# Don't return if failed to parse any part
	if (title and body and image):
		return {
			'title': title,
			'body': body,
			'image': image
		}
	else:
		return None


# Wrapper for redis queries related to articles
class RedisClient(object):
	# Delete articles after 3 days
	ARTICLE_TTL = 3 * 24 * 60 * 60

	def __init__(self, host='localhost', port=6379, db=0):
		self.client = redis.StrictRedis(host=host, port=port, db=db)

	def get_all_sources(self):
		sources = []

		for source_key in self.client.scan_iter('src:*'):
			source = json.loads(self.client.get(source_key))
			sources.append(source)

		return sources

	def has_article(self, link):
		key_by_link = 'news:by_link:%s' % link
		return self.client.exists(key_by_link)

	def add_article(self, article):
		# assign id
		next_id = int(self.client.get('news:next_id'))
		self.client.incr('news:next_id')
		article['id'] = next_id

		# add to query full article by id
		key_by_id = 'news:by_id:%s' % next_id
		article_json = json.dumps(article)
		self.client.set(key_by_id, article_json, ex=self.ARTICLE_TTL)

		# add to query presense by link
		key_by_link = 'news:by_link:%s' % article['link']
		self.client.set(key_by_link, 1, ex=self.ARTICLE_TTL)

		return next_id

	def get_article(self, _id):
		key_by_id = 'news:by_id:%s' % _id
		return self.client.get(key_by_id)


# Wrapper around a RabbitMQ producer channel;
# Sets up & tears down connection;
class RabbitMQClient(object):
	def __init__(self, host, queue):
		self.conn = pika.BlockingConnection(
			pika.ConnectionParameters(
				host=rabbitmq_settings['host']
			)
		)

		self.queue = queue

		self.ch = self.conn.channel()
		self.ch.queue_declare(
			queue=self.queue,
			durable=True
		)

	def produce(self, message):
		self.ch.basic_publish(
			exchange='',
			routing_key=self.queue,
			body=message,
			properties=pika.BasicProperties(
				delivery_mode = 2, # make message persistent
			)
		)

	def disconnect(self):
		self.conn.close()
		self.conn = None
		self.queue = None
		self.ch = None


if __name__ == '__main__':
	db = RedisClient()
	tagger_conn = RabbitMQClient(rabbitmq_settings['host'], amqp_addr('ms-tagger'))

	for source in db.get_all_sources():
		links = download_article_links(source)

		print 'Fetched %d links from %s:' % (len(links), source['name'])
		pprint(links)

		# additional 'source' field to add to articles w/o the parsing info
		source_clone = copy.deepcopy(source)
		source_clone.pop('parsing_data', None)

		for link in links:
			# Check if already downloaded
			if db.has_article(link):
				print 'Ignoring: %s' % link
				continue

			article = download_article(source, link)
			if article:
				print 'Adding: %s' % link
				article['link'] = link
				article['source'] = source_clone
				article['timestamp'] = datetime.datetime.utcnow().strftime("%Y-%m-%d %H:%M:%S")

				_id = db.add_article(article)
				article['id'] = _id
				print 'Saved to db'

				tagger_conn.produce(json.dumps(article))
				print 'Sent to tagger'

			else:
				print 'Unparseable: %s' % link

	tagger_conn.disconnect()


