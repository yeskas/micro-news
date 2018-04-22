-- These are cql queries, but using the .sql extension for the IDE

DROP TABLE rs.users;
DROP TABLE rs.user_tags;
DROP TABLE rs.articles;
DROP TABLE rs.article_tags;
DROP TABLE rs.clusters;
DROP TABLE rs.cluster_tags;

CREATE TABLE users (
	id INT PRIMARY KEY,
	cluster_id INT
);

CREATE TABLE user_tags (
	id INT PRIMARY KEY,
    art INT, business INT, comics INT, creativity INT, crime INT, cryptography INT, culture INT, cyber INT, data INT, design INT, digital INT, economy INT, education INT, entrepreneurship INT, environment INT, equality INT, family INT, film INT, finance INT, food INT, freelance INT, future INT, gun INT, health INT, history INT, humor INT, income INT, javascript INT, leadership INT, literature INT, marketing INT, math INT, media INT, music INT, neuroscience INT, parenting INT, philosophy INT, photography INT, politics INT, productivity INT, programming INT, psychology INT, relationships INT, science INT, security INT, self INT, social INT, software INT, space INT, spirituality INT, sports INT, technology INT, travel INT, wellness INT, work INT, world INT, writing INT
);

CREATE TABLE articles (
	id INT PRIMARY KEY,
    article_json TEXT
);

CREATE TABLE article_tags (
	id INT PRIMARY KEY,
    art INT, business INT, comics INT, creativity INT, crime INT, cryptography INT, culture INT, cyber INT, data INT, design INT, digital INT, economy INT, education INT, entrepreneurship INT, environment INT, equality INT, family INT, film INT, finance INT, food INT, freelance INT, future INT, gun INT, health INT, history INT, humor INT, income INT, javascript INT, leadership INT, literature INT, marketing INT, math INT, media INT, music INT, neuroscience INT, parenting INT, philosophy INT, photography INT, politics INT, productivity INT, programming INT, psychology INT, relationships INT, science INT, security INT, self INT, social INT, software INT, space INT, spirituality INT, sports INT, technology INT, travel INT, wellness INT, work INT, world INT, writing INT
);

CREATE TABLE clusters (
	id INT PRIMARY KEY,
	articles_json TEXT,
	scores_json TEXT
);

CREATE TABLE cluster_tags (
	id INT PRIMARY KEY,
    art DOUBLE, business DOUBLE, comics DOUBLE, creativity DOUBLE, crime DOUBLE, cryptography DOUBLE, culture DOUBLE, cyber DOUBLE, data DOUBLE, design DOUBLE, digital DOUBLE, economy DOUBLE, education DOUBLE, entrepreneurship DOUBLE, environment DOUBLE, equality DOUBLE, family DOUBLE, film DOUBLE, finance DOUBLE, food DOUBLE, freelance DOUBLE, future DOUBLE, gun DOUBLE, health DOUBLE, history DOUBLE, humor DOUBLE, income DOUBLE, javascript DOUBLE, leadership DOUBLE, literature DOUBLE, marketing DOUBLE, math DOUBLE, media DOUBLE, music DOUBLE, neuroscience DOUBLE, parenting DOUBLE, philosophy DOUBLE, photography DOUBLE, politics DOUBLE, productivity DOUBLE, programming DOUBLE, psychology DOUBLE, relationships DOUBLE, science DOUBLE, security DOUBLE, self DOUBLE, social DOUBLE, software DOUBLE, space DOUBLE, spirituality DOUBLE, sports DOUBLE, technology DOUBLE, travel DOUBLE, wellness DOUBLE, work DOUBLE, world DOUBLE, writing DOUBLE
);
