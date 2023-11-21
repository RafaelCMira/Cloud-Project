call mvn clean compile package
call cp ../target/*.war .
call docker build -t rlpereirafct2002/scc60700-app .
