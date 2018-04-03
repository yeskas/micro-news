from flask import Flask
from flask import render_template


app = Flask(__name__)


# Landing page with suggested news and user info
@app.route('/')
def index():
	items = [
		{
			'title': 'Masters: Tiger Woods, Rory McIlroy, Justin Rose tee times announced',
			'subtitle': 'Four-time winner Tiger Woods will be among the early starters when the Masters at Augusta begins on Thursday.',
			'image': 'https://ichef.bbci.co.uk/onesport/cps/800/cpsprodpb/01E8/production/_100688400_tiger_reuters.jpg',
			'link': 'http://www.bbc.com/sport/golf/43634645',
			'source': 'TechCrunch',
			'source_icon': 'http://bocacommunications.com/wp-content/uploads/2017/07/TechCrunch-Logo.jpg'
		},
		{
			'title': 'New Zealand v England: Ish Sodhi guides hosts to draw and series win',
			'subtitle': 'New Zealand held their nerve to secure a dramatic draw against England in the second Test in Christchurch and claim a 1-0 series victory.',
			'image': 'https://ichef.bbci.co.uk/onesport/cps/800/cpsprodpb/12DF0/production/_100669277_englandfield_getty.jpg',
			'link': 'http://www.bbc.com/sport/cricket/43624239',
			'source': 'BBC',
			'source_icon': 'http://icons.iconarchive.com/icons/martz90/circle/512/bbc-news-icon.png'
		},
		{
			'title': 'Miami Open: John Isner beats Alexander Zverev to win first Masters 1,000 title',
			'subtitle': 'American John Isner won his first ever Masters 1,000 title with a 6-7 (4-7) 6-4 6-4 victory over German Alexander Zverev in the Miami Open final.',
			'image': 'https://ichef.bbci.co.uk/onesport/cps/800/cpsprodpb/17ABB/production/_100655969_isner_reuters.jpg',
			'link': 'http://www.bbc.com/sport/tennis/43613957',
			'source': 'TechCrunch',
			'source_icon': 'http://bocacommunications.com/wp-content/uploads/2017/07/TechCrunch-Logo.jpg'
		}
	] * 4
	return render_template('news.html', items=items)


# User clicked on news item to read
# TODO: remove GET method
@app.route('/item_clicked', methods=['GET', 'POST'])
def item_clicked():
	return 'All tags of the news incremented for user'


# User removed the news item from feed
# TODO: remove GET method
@app.route('/item_removed', methods=['GET', 'POST'])
def item_removed():
	return 'All tags of the news decremented for user'
