import requests

all_words = [
	"Neuroscience",
	"Javascript",
	"Literature",
	"Technology",
	"Family",
	"Software"
]

for word in all_words:
	word = word.lower()
	r = requests.get('http://semantic-link.com/related.php', params={'word': word})
	links = r.json()[:5]

	# Generate some scala code
	print '"%s"->Array(' % word
	for link in links:
		v = link['v'].lower()
		score = float(link['mi_norm'])
		print '\tWordLink("%s", %s),' % (v, score)
	print '),\n'


