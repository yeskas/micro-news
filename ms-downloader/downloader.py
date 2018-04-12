import json
import pdb
from pprint import pprint

from pyquery import PyQuery


NEWS_SOURCES = [
	{
		"name": "TechCrunch",
		"link": "https://techcrunch.com",
		"icon": "http://bocacommunications.com/wp-content/uploads/2017/07/TechCrunch-Logo.jpg",

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
	}
]


# Fetch the list of latest news based on the source-specific configs
def download_article_links(source):
	url = source['parsing_data']['list']['url']
	sel = source['parsing_data']['list']['selectors']

	# 'pq' acts like jquery's '$'
	pq = PyQuery(url=url)

	link_elems = pq(sel['article_link'])
	links = [elem.attrib['href'] for elem in link_elems]

	return links


# Download and parse the data of interest of a specific news item
# based on the source-specific configs
def download_article(source, link):
	print 'Downloading from %s' % link

	# 'pq' acts like jquery's '$'
	pq = PyQuery(url=link)
	sel = source['parsing_data']['article']['selectors']

	title = pq(sel['title']).text()

	body = pq('.article-content').text()
	# body = pq('.article-content').text().encode('utf-8')

	image_elems = pq(sel['image'])
	if len(image_elems) > 0:
		image = image_elems[0].attrib['src']
	else:
		image = ''


	return {
		'title': title,
		'body': body,
		'image': image
	}


articles = []
for source in NEWS_SOURCES:
	links = download_article_links(source)
	print links

	# for link in links:
	for link in links:
		article = download_article(source, link)
		article['link'] = link
		print 'Adding new article:'
		pprint(article)

		articles.append(article)


	# tmp dump to json
	with open('news_items.json', 'w') as outfile:
		json.dump({'news_items': articles}, outfile)
