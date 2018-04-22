-- Default cluster that just has the latest news
insert into clusters(id, articles_json) values (0, '[]');
insert into cluster_tags(id) values (0);
