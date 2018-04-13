import json
import pdb
from pprint import pprint

import redis


INIT_NEWS_SOURCES = [
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


# Run this only once to initialize the news sources in the db
if __name__ == '__main__':
	redis_client = redis.StrictRedis(host='localhost', port=6379, db=0)

	for source in INIT_NEWS_SOURCES:
		redis_key = 'src:%s' % source['code']
		redis_client.set(redis_key, json.dumps(source))




