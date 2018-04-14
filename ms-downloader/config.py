rabbitmq_settings = {
	'host': 'localhost'
}

service_registry = {
	'ms-tagger': {
		'amqp': {
			'queue': 'downloader:tagger:articles'
		}
	}
}
