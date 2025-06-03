./gradlew clean assemble
APP_VERSION=$(grep '^version=' gradle.properties| cut -d '=' -f2-)
export APP_VERSION
docker-compose build --no-cache
docker-compose up
