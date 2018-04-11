-- These are cql queries, but using the .sql extension for the IDE

CREATE TABLE users (
	id INT PRIMARY KEY,
	cluster_id INT
);

CREATE TABLE articles (
	id INT PRIMARY KEY,
    body_json TEXT
);

CREATE TABLE clusters (
	id INT PRIMARY KEY,
	articles_json TEXT
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

CREATE TABLE article_tags (
	id INT PRIMARY KEY,
	neuroscience INT,
	javascript INT,
	literature INT,
	technology INT,
	family INT,
	software INT
);

CREATE TABLE cluster_tags (
	id INT PRIMARY KEY,
	neuroscience DOUBLE,
	javascript DOUBLE,
	literature DOUBLE,
	technology DOUBLE,
	family DOUBLE,
	software DOUBLE
);
