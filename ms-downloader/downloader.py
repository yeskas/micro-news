import json
import pdb
from pprint import pprint
import re

from pyquery import PyQuery
import redis


# Delete articles after 3 days
ARTICLE_TTL = 3 * 24 * 60 * 60

# TODO: put this into db
NEWS_SOURCES = [
	{
		"name": "TechCrunch",
		"code": "techcrunch",
		"link": "https://techcrunch.com",
		"icon": "https://techcrunch.com/wp-content/uploads/2015/02/cropped-cropped-favicon-gradient.png?w=192",

		"parsing_data": {
			# Data about the page that contains the list of latest articles
			"list": {
				"url": "https://techcrunch.com",
				"selectors": {
					"article_link": "a.post-block__title__link"
				}
			},

			# Data about the page that contains a single article
			"article": {
				"selectors": {
					"title": ".article__title",
					"body":  ".article-content",
					"image": ".article__featured-image",
				}
			}
		}
	},

	{
		"name": "BBC",
		"code": "bbc",
		"link": "http://bbc.com",
		"icon": "https://ichef.bbci.co.uk/images/ic/192xn/p05hy0qr.jpg",

		"parsing_data": {
			# Data about the page that contains the list of latest articles
			"list": {
				"url": "http://www.bbc.com/news",
				"selectors": {
					"article_link": ".nw-c-most-read__items a.gs-c-promo-heading"
				}
			},

			# Data about the page that contains a single article
			"article": {
				"selectors": {
					"title": ".story-body__h1",
					"body":  ".story-body__inner p",
					"image": ".image-and-copyright-container img.js-image-replace",
				}
			}
		}
	},

	{
		"name": "Bloomberg",
		"code": "bloomberg",
		"link": "https://www.bloomberg.com",
		"icon": "https://assets.bwbx.io/business/public/images/favicons/favicon-192x192-3621dae772.png",

		"parsing_data": {
			# Data about the page that contains the list of latest articles
			"list": {
				"url": "https://www.bloomberg.com/markets",
				"selectors": {
					"article_link": "a.top-news-v3-story-headline__link"
				}
			},

			# Data about the page that contains a single article
			"article": {
				"selectors": {
					"title": ".lede-large-content__highlight, .lede-text-only__highlight",
					"body":  ".fence-body p",
					"image": ".bg-crop--1x-1 div.lede-large-image__image, .video-player__container",
				}
			}
		}
	},

	{
		"name": "Sky Sports",
		"code": "skysports",
		"link": "http://www.skysports.com",
		"icon": "http://www.sky.com/assets/masthead/images/sky-logo.png",

		"parsing_data": {
			# Data about the page that contains the list of latest articles
			"list": {
				"url": "http://www.skysports.com/news-wire",
				"selectors": {
					"article_link": "a.news-list__figure"
				}
			},

			# Data about the page that contains a single article
			"article": {
				"selectors": {
					"title": ".article__title",
					"body":  ".article__body p",
					"image": "img.widge-figure__image",
				}
			}
		}
	}
]


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

	for source in NEWS_SOURCES:
		links = download_article_links(source)

		print '\nFetched %d links for %s:' % (len(links), source['name'])
		pprint(links)

		for link in links:
			redis_key = 'news:%s:%s' % (source['code'], link)

			# Check if already downloaded
			if redis_client.exists(redis_key):
				print 'Ignoring: %s' % link
				continue

			article = download_article(source, link)
			if article:
				print 'Adding: %s' % link
				article['link'] = link
				redis_client.set(redis_key, json.dumps(article), ex=ARTICLE_TTL)
			else:
				print 'Unparseable: %s' % link




