import json
import pdb
from pprint import pprint
import re


import pika
from pyquery import PyQuery
import redis

from config import rabbitmq_settings, service_registry


# Delete articles after 3 days
ARTICLE_TTL = 3 * 24 * 60 * 60


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


if __name__ == '__main__':
	redis_client = redis.StrictRedis(host='localhost', port=6379, db=0)
	# TODO: move into separate class
	queue_conn = pika.BlockingConnection(
		pika.ConnectionParameters(
			host=rabbitmq_settings['host']
		)
	)
	channel = queue_conn.channel()
	channel.queue_declare(
		# TODO: redo confg format
		queue=service_registry['ms-tagger']['amqp']['queue'],
		durable=True
	)


	for source_key in redis_client.scan_iter("src:*"):
		source = json.loads(redis_client.get(source_key))
		links = download_article_links(source)

		print 'Fetched %d links for %s:' % (len(links), source['name'])
		pprint(links)

		for link in links:
			redis_key = 'news:%s:%s' % (source['code'], link)

			# Check if already downloaded
			if redis_client.exists(redis_key):
				print '\tIgnoring: %s' % link
				continue

			article = download_article(source, link)
			if article:
				print '\tAdding: %s' % link
				article['link'] = link
				article_json = json.dumps(article)

				redis_client.set(redis_key, article_json, ex=ARTICLE_TTL)
				print '\t\tsaved to db'

				channel.basic_publish(
					exchange='',
					routing_key=service_registry['ms-tagger']['amqp']['queue'],
					body=article_json,
					properties=pika.BasicProperties(
						delivery_mode = 2, # make message persistent
					)
				)
				print '\t\tsent to tagger'

			else:
				print '\tUnparseable: %s' % link

	queue_conn.close()


