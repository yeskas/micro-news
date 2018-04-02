from flask import Flask
from flask import render_template


app = Flask(__name__)


# Landing page with suggested news and user info
@app.route('/')
def index():
	return render_template('news.html')


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
