FROM openjdk:11
COPY . /app
WORKDIR /app
RUN javac -cp "lib/*" src/org/cloudbus/cloudsim/Main.java
CMD ["java", "-cp", "src:lib/*", "org.cloudbus.cloudsim.Main"]