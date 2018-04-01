from flask import Flask
app = Flask(__name__)


# Landing page with suggested news and user info
@app.route('/')
def index():
	return 'Hello, World! Welcome to micro-news!'


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
