service_registry = {
	'ms-user-mgmt': {
		'http': {
			'host': 'localhost',
			'port': 8080
		}
	},

	'ms-rec-sys': {
		'http': {
			'host': 'localhost',
			'port': 9000
		}
	}
}

# Builds http address from configs in the service registry
def http_addr(service_name):
	return 'http://%s:%s' % (\
		service_registry[service_name]['http']['host'],\
		service_registry[service_name]['http']['port']\
	)
