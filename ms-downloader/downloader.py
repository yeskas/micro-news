from xml.etree import ElementTree
from lxml.html import HtmlElement as HE
from pyquery import PyQuery as pq
import pdb
import json


# Fetch the list of latest news based on the source-specific configs
def download_news_links(news_source):
	# 'd' acts like jquery's '$'
	d = pq(url='https://techcrunch.com')

	link_elems = d('a.post-block__title__link')
	links = [elem.attrib['href'] for elem in link_elems]

	return links


# Download and parse the data of interest of a specific news item
# based on the source-specific configs
def download_news_item(news_source, link):
	print 'Downloading from %s' % link

	# 'd' acts like jquery's '$'
	d = pq(url=link)

	title = d('.article__title').text()

	img_link_elems = d('.article__featured-image')
	if len(img_link_elems) > 0:
		img_link = img_link_elems[0].attrib['src']
	else:
		img_link = ''

	body = d('.article-content').text()
	# body = d('.article-content').text().encode('utf-8')

	return {
		'title': title,
		'img_link': img_link,
		'body': body
	}


news_sources = ['techcrunch']
for news_source in news_sources:
	all_items = []

	links = download_news_links(news_source)
	print links

	# for link in links:
	for link in links[:3]:
		item = download_news_item(news_source, link)
		item['link'] = link

		all_items.append(item)
		print '\n\n' + '+' * 40
		print item['link']
		print '\n\n' + '+' * 40
		print item['title']
		print '\n\n' + '+' * 40
		print item['img_link']
		print '\n\n' + '+' * 40
		print item['body']
		print '\n\n' + '+' * 40

		# break


	# tmp dump to json
	with open('news_items.json', 'w') as outfile:
		json.dump({'news_items': all_items}, outfile)




