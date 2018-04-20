# micro-news

This project is comprised of 5 services:
- ms-gateway: forwards APIs from the web UI to backend services
- ms-user-mgmt: CRUD manager for user accounts
- ms-downloader: regularly updates the database of news links
- ms-tagger: assigns tags (topics) to articles
- ms-rec-sys: recommends news to users based on their past habits

References:
- tag list is based on topics on medium.com
- word links (used for tagging) are fetched from this awesome website: semantic-link.com
- the current list of news sources: BBC, TechCrunch, Bloomberg, Sky Sports
