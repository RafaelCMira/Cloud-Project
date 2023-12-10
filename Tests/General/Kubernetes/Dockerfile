FROM node:18.18.1

WORKDIR /app

COPY package*.json ./

RUN npm install -g artillery && \
    npm install -g faker@5.5.3 && \
    npm install -g node-fetch && \
    npm install -g https://github.com/preguica/artillery-plugin-metrics-by-endpoint.git

COPY ../Data/*.data ./Tests/Data/
COPY ../*.js ./Tests/General/
#COPY ./experimentWestEuropeCache.yml ./Tests/General/Kubernetes/

# Define a volume to store the output file
VOLUME /app/output

CMD ["artillery", "run", "--output", "/app/output/experimentWestEuropeCache.json", "experimentWestEuropeCache.yml"]
