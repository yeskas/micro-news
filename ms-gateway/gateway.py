import json
import pika
import requests

from flask import Flask, render_template, g
from flask import request, Response, session

from config import rabbitmq_settings, http_addr, amqp_addr


app = Flask(__name__)

# Encrypt sessions with this
from secret_key import secret_key
app.secret_key = secret_key


# Wrapper around article helper methods
class Article(object):
	MAX_SUBTITLE_LEN = 200

	# Extract the subtitle sentence of article's body
	@classmethod
	def extract_subtitle(cls, body):
		body = body[:cls.MAX_SUBTITLE_LEN]

		# First sentence on same line
		dot_idx = body.find('. ')
		if dot_idx >= 0:
			return body[:(dot_idx + 1)]

		# First sentence on different line
		nl_idx = body.find('.\n')
		if nl_idx >= 0:
			return body[:(nl_idx + 1)]

		# Cut at last space
		space_idx = body.rfind(' ')
		if space_idx >= 0:
			# cut at last space
			return body[:(space_idx + 1)] + '...'

		# Cut at last char
		return body + '...'


# Store connection to rabbitmq & ms-rec-sys channel in app context
def get_rabbitmq_channel():
	ch = getattr(g, '_rabbitmq_channel', None)

	if ch is None:
		conn = pika.BlockingConnection(
			pika.ConnectionParameters(
				host=rabbitmq_settings['host']
			)
		)
		ch = conn.channel()
		ch.queue_declare(
			queue=amqp_addr('ms-rec-sys'),
			durable=True
		)

		g._rabbitmq_conn = conn
		g._rabbitmq_channel = ch

	return ch


# Close rabbitmq connection @ teardown
@app.teardown_appcontext
def teardown_rabbitmq_channel(exception):
	conn = getattr(g, '_rabbitmq_conn', None)
	if conn is not None:
		conn.close()


# Landing page with suggested news and user info
@app.route('/')
def index():
	# Check with & fetch data from ms-user-mgmt
	if 'user_id' in session:
		# retrieve existing user
		user = requests.get(
			'%s/users/get' % http_addr('ms-user-mgmt'),
			params={'id': session['user_id']}
		).json()

		user['is_new'] = False

	else:
		# save new user
		user = requests.post(
			'%s/users/add' % http_addr('ms-user-mgmt'),
			data={}
		).json()

		user['is_new'] = True

		# store id in session
		session['user_id'] = user['id']

	# Fetch the latest news recommended for this user from ms-rec-sys
	items = requests.get(
		'%s/rec' % http_addr('ms-rec-sys'),
		params={'userId': session['user_id']}
	).json()

	# Change format for the view
	for item in items:
		body = item.pop('body', '')
		if 'subtitle' not in item:
			item['subtitle'] = Article.extract_subtitle(body)

	return render_template('news.html', user=user, items=items)


# User changed their name in the modal
@app.route('/set_user_name', methods=['POST'])
def set_user_name():
	if 'user_id' not in session:
		abort(401)

	user_name = request.form.get('user_name', '').strip()
	if user_name == '':
		# return as invalid
		data = {
			'user_name': '',
			'is_valid': False,
		}

	else:
		# save to ms-user-mgmt & return as valid
		requests.post(
			'%s/users/edit' % http_addr('ms-user-mgmt'),
			data={'id': session['user_id'], 'name': user_name}
		).json()

		data = {
			'user_name': user_name,
			'is_valid': True,
		}

	return Response(
		json.dumps(data),
		status=200,
		mimetype='application/json'
	)


# User clicked on news item to read
@app.route('/item_clicked', methods=['POST'])
def item_clicked():
	if 'user_id' not in session:
		abort(401)

	article_id = int(request.form.get('article_id', 0))

	ch = get_rabbitmq_channel()
	ch.basic_publish(
		exchange='',
		routing_key=amqp_addr('ms-rec-sys'),
		body=json.dumps({
			'userId': session['user_id'],
			'articleId': article_id
		}),
		properties=pika.BasicProperties(
			delivery_mode = 2, # make message persistent
		)
	)

	return ''
