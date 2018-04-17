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

# Returns queue name from configs in the service registry
def amqp_addr(service_name):
	return service_registry[service_name]['amqp']['queue']
