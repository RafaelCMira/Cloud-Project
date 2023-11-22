call mvn clean compile package
:: You can change the name following the -t flag to give a different name to your image
call docker build -t rlpereirafct2002/scc60700-app ./docker
call docker push rlpereirafct2002/scc60700-app