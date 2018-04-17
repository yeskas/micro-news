#!/usr/bin/env python
import json
import pdb
import requests


# This script builds a json object mapping each tag to its related words
tag_objects = []

for line in open('tag_names.txt'):
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

	tag_objects.append({
		"word": tag,
		"links": links
	})

# Print so that it's semi-beautified
print '[\n\t' + ',\n\t'.join(json.dumps(to) for to in tag_objects) + '\n]'
