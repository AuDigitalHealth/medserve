[![CircleCI](https://circleci.com/gh/AuDigitalHealth/medserve.svg?style=shield)](https://circleci.com/gh/AuDigitalHealth/medserve)

# Medserve #

Medserve is an *experimental* FHIR server *prototype* which serves up Medication, Substance and Organisation resources built from terminology and other data sources. At present it serves a set of Medication resources which are built from AMT with PBS data blended in. Future plans involve importing TGA data, dm+d and RxNorm.

Medserve is currently *not* part of any production infrastructure.

Medserve has been built to explore the idea of presenting medicines terminologies (AMT, dm+d, RxNorm...etc) via a common API (FHIR). This is intended to make it easier
 - to implement each individually through an API rather than starting from native file formats and building supporting infrastructure
 - and to implement multiple medicines terminologies because there is a common API to each of them

Feedback is very much appreciated to help guide where this project should go - please raise an issue with any feedback on the concept, implementation, API etc.

## How to run it ##

You can run Medserve from prebuilt Docker images on Docker Hub built from master by CircleCI. There is a Docker Compose file in the ./server directory you can use to start up Medserve with its website and Polecat browser.

## Building it yourself ##

Building the containers yourself isn't hard. Prequisites include the JDK (obviously), Maven and Docker.

Clone the repository and run
```bash
mvn install -Dsynd.client.id=your_client_id -Dsynd.client.secret=your_client_secret -Dsynd.cache.dir=/path/to/keep/terminology/files
```

This will build and locally install the Docker images, you will need a set of client credentials to download content from the NCTS as this process will download the latest version of AMT and the PBS. Refer to https://www.healthterminologies.gov.au/specs/v2/national-services/api-security for more details on getting client credentials.

You'll also need to specify where to download these files to, which if available between runs will be consulted before new files are downloaded and can speed up the process.

To make execution easier (if you're not too paranoid) you can set these values into environment variables - e.g.
```bash
export SYND_CACHE_DIR=/path/to/keep/terminology/files
export SYND_CLIENT_ID=your_client_id
export SYND_CLIENT_SECRET=your_client_secret
```

Then you can simply run
```bash
mvn install
```

## API ##

The API exposed is currently best described by the [Postman collection](https://documenter.getpostman.com/view/2091243/medserve/77k3MAR)
