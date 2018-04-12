#!/usr/bin/env python
import requests
import json


# This script builds a json object mapping each tag to its related words
print '{'
for line in open('all_tags.txt'):
	# Add the tag itself
	tag = line[:-1]
	links = [{'word': tag, 'score': 1.0}]

	# print 'Querying words related to %s' % tag
	r = requests.get('http://semantic-link.com/related.php', params={'word': tag})
	raw_links = r.json()

	found_words = []
	for raw_link in raw_links:
		word = raw_link['v'].lower()
		score = float(raw_link['mi_norm'])

		# ignore duplicates
		if word not in found_words:
			links.append({
				'word': word,
				'score': score
			})

	print '\t"%s": %s,' % (tag, json.dumps(links))
print '}'
