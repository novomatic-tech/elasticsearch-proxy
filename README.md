# Elasticsearch Proxy

[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=novomatic-tech_elasticsearch-proxy&metric=alert_status)](https://sonarcloud.io/dashboard?id=novomatic-tech_elasticsearch-proxy)
[![Build Status](https://travis-ci.org/novomatic-tech/elasticsearch-proxy.svg?branch=master)](https://travis-ci.org/novomatic-tech/elasticsearch-proxy)
[![Docker Pulls](https://img.shields.io/docker/pulls/novomatic/elasticsearch-proxy.svg)](https://hub.docker.com/r/novomatic/elasticsearch-proxy)

A proxy that provides authentication and document level security for Elasticsearch.
As of now, it is integrated with OAuth2.0-compliant [Keycloak Authorization Server](https://www.keycloak.org/).

Before running the example, make sure you have a decent understanding of 
core [OAuth2.0 protocol concepts](https://www.digitalocean.com/community/tutorials/an-introduction-to-oauth-2).

## Running

Run using the default configuration:

```bash
docker run -d -p 19200:19200 novomatic/elasticsearch-proxy
```

Run and configure by mounting an external file:

```bash
docker run -d -p 19200:19200 -v application.yaml:/opt/elasticsearch-proxy/application.yaml novomatic/elasticsearch-proxy
```

## Configuration

The application is configured by the following configuration file: `application.yaml`.

The `keycloak` property in the `application.yaml` file defines the configuration 
of [the Keycloak Adapter](https://www.keycloak.org/docs/latest/securing_apps/#java-adapters).

The `elasticsearch.proxy.security.allow` property in the `application.yaml`
file defines authorization rules for documents stored in Elasticsearch.

The following sample configuration briefly explains how 
the document-level security works:

```yaml
server:
  # The port the Elasticsearch proxy should be run on.
  # This port should be used to connect to Elasticsearch with Document-level security.
  port: 19200
  
zuul:
  routes:
    es:
      # The base url of the upstream (unprotected) Elasticsearch.
      url: http://localhost:9200/
      
elasticsearch:
  proxy:
     # This section defines authorization rules
     security:
     
       # Allow any operation on Elastic cluster for users with 'manage-kibana' client role for the 'kibana' client  
       # Users matching this rule effectively use the upstream Elasticsearch as it was unprotected. 
       admin:
         roles: kibana.manage-kibana
         
       allow:
       # Allow all authenticated users
       # to read documents
       # from '.kibana' index with property 'type' set to one of (index-pattern, config)
       - actions: read
         resources:
           indices: .kibana
           query: "type:index-pattern OR type:config" # This is a lucene query
          
       # Allow users with 'view-dashboards' client role for the 'kibana' client
       # to read documents
       # from '.kibana' index with property 'type' set to 'dashboard'
       - principal:
           roles: kibana.view-dashboards
         actions: read
         resources:
           indices: .kibana
           query: "type:dashboard"
 
       # Allow users with 'manage-dashboards' client role for the 'kibana' client
       # to create, update and delete documents
       # from '.kibana' index 
       # but only when a document has a 'createdBy' field set to the username
       - principal:
           roles: kibana.manage-dashboards
         actions: write
         resources:
           indices: .kibana
           queryScript: > 
             [ createdBy: principal.token.preferredUsername ] 
```

Document-level security is achieved by setting one of the following:

- `resources.query` - a lucene query string limiting the documents
- `resources.queryScript` - a groovy script returning a lucene query limiting the documents
  
  Each script has the following variables predefined: 
  
  - `request` - an object representing the current request (see the [ElasticsearchRequest](src/main/java/com/novomatic/elasticsearch/proxy/ElasticsearchRequest.java) class for details)
  - `principal` - an object representing the current user (see the [Principal](src/main/java/com/novomatic/elasticsearch/proxy/Principal.java) class for details).
     It includes a `token` property which represents the user's access token used in the request
     (see [AccessToken class](https://www.keycloak.org/docs-api/3.4/javadocs/org/keycloak/representations/AccessToken.html) for available token properties)
  - `matchedRules` - a list of rules matching the current request context (filtered from all `allow` rules)
  - `matchedIndices` - a set of indices accessible for the user in the current request context (determined from all `allow` rules)
  
  Based on this information, the query script should return one of the following:
  
  - a map of terms to be matched (terms will be joined with AND logic, and a corresponding lucene query will be built)
  - a lucene query string
  - a null value indicating that the user does not have access to resource

When neither `query` nor `queryScript` is set, the request will be processed for all documents from `resources.indices`.
Document-level security works for both read and write operations.

## Running from sources

1. Run the required components:

   ```yaml
   docker-compose up -d
   mvn spring-boot:run
   ```

2. Obtain the access token for the `jdoe:jdoe` user:

   ```
   POST http://localhost:8080/auth/realms/example/protocol/openid-connect/token
   Content-Type: application/x-www-form-urlencoded
   
   username=jdoe&password=jdoe&grant_type=password&client_id=kibana&client_secret=y8gSCns8hPTkQr6Zqwu2hPw6ScQDZNZz
   ```

   You can visit [jwt.io](http://jwt.io/) and paste the received access token there to 
   see how tokens issued by Keycloak encapsulate information useful 
   for performing Role-based Access Control.

3. Send a request to Elasticsearch proxy. 
   The only difference is that you have to add `Authorization: Bearer ...` header:

   ```
   POST http://localhost:19200/.kibana/_search
   Authorization: Bearer [copy value from received 'accessToken' property here]
   Content-Type: application/json
   ```

## Versioning

This project is in line with the best practices from Semantic Versioning. An unstable version is considered when it ends with the `-SNAPSHOT` fragment. Otherwise, the version is considered stable.

### Docker tags

A stable versions eg. `1.2.3` will create the following tags for the docker image: `stable`, `latest`, `1.2.3`, `1.2`, `1`.

An unstable versions eg. `1.2.3-SNAPSHOT` will create the following tags for the docker image: `unstable`, `1.2.3-SNAPSHOT`, `1.2.3-g0a51d42`, where `0a51d42` is a short commit hash.

## Building

To build a jar file:

```bash
mvn clean package
```

To build a docker image:

```bash
mvn clean install
```

To publish a docker image to the registry:

```bash
docker login
mvn git-commit-id:revision groovy:execute@docker-tags docker:push
```
