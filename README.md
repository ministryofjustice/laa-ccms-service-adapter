# laa-ccms-service-adapter

The service adapter is a Spring Boot mircoservice built for processing OPA 18 AssessService requests for means, merits and billing rulebases.

# Technologies Used
1. Java 8
2. Spring boot
3. Apache CXF
4. OPA 12 AssessService
5. Maven

## How to build and run locally

To build the application:  
`./mvnw clean install`

Then to run this application in Docker:  
`docker-compose up`

To run the Cypress tests:  
First run `npm install`. You can then run the test suite non-interactively with `npm test`.

Use `$(npm bin)/cypress open` to open the Cypress.io test runner. This allows you to view the browser and the test report side by side and see what's happening when a test fails.

## Code style
We are following [Google's style guide for Java](https://google.github.io/styleguide/javaguide.html).

To ensure your code is formatted correctly, import one of these formatters into your IDE:

- [Intellij formatter](https://raw.githubusercontent.com/google/styleguide/gh-pages/intellij-java-google-style.xml)
- [Eclipse formatter](https://raw.githubusercontent.com/google/styleguide/gh-pages/eclipse-java-google-style.xml)

Import it by going to `Code Style -> Java` (or `Java -> Code Style`) in the preferences.

