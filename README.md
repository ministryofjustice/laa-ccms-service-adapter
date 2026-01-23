# laa-ccms-service-adapter

The service adapter is a Spring Boot microservice built for processing OPA 18 AssessService requests for means, merits and billing rulebases.

# Technologies Used
1. Java 8
2. Spring boot
3. Apache CXF
4. OPA 12 AssessService
5. Gradle

## How to build and run locally

To build the application:  
`./gradlew clean build`

Then to run this application in Docker:
`docker-compose up`

## Code style
We are following [Google's style guide for Java](https://google.github.io/styleguide/javaguide.html).

To ensure your code is formatted correctly, import one of these formatters into your IDE:

- [IntelliJ formatter](https://raw.githubusercontent.com/google/styleguide/gh-pages/intellij-java-google-style.xml)
- [Eclipse formatter](https://raw.githubusercontent.com/google/styleguide/gh-pages/eclipse-java-google-style.xml)

Import it by going to `Code Style -> Java` (or `Java -> Code Style`) in the preferences.

