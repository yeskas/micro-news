CREATE TABLE news (
	id INT PRIMARY KEY,
	title TEXT,
	body TEXT,
	link TEXT,
	img_link TEXT
);

CREATE TABLE user_tags (
	id INT PRIMARY KEY,
	neuroscience INT,
	javascript INT,
	literature INT,
	technology INT,
	family INT,
	software INT
);

CREATE TABLE news_tags (
	id INT PRIMARY KEY,
	neuroscience INT,
	javascript INT,
	literature INT,
	technology INT,
	family INT,
	software INT
);

CREATE TABLE cluster_weights (
	id INT PRIMARY KEY,
	neuroscience DOUBLE,
	javascript DOUBLE,
	literature DOUBLE,
	technology DOUBLE,
	family DOUBLE,
	software DOUBLE
);

CREATE TABLE cluster_news (
	id INT PRIMARY KEY,
	news_json TEXT
);
