FROM node:12.18.1

ADD nodeFiles .

RUN npm install

CMD ["artillery" , "run", "--output", "experimentWestEuropeCache.json", "experimentWestEuropeCache.yml"]