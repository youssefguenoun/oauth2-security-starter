## Overview
 
This module let you **protect your API** by requiring a **valid access token for each request**.
 
* That means that **each request coming in your service will be validated before being executed**.
 
* To do so, this module **ask the Mitreid authorization server** if the access token given by your the request is valid.
 
* Then, this module can **check if the scopes linked to the access token match all the _required scopes_ of your api operation(s)**.
 
 
## In details
 
For a detailed understanding, this module, once enabled:
 
* Inspect every incoming http request for oauth2 _Authorization: Bearer_ http header (carrying the access token).
* Validate access token against Mitreid oauth2 authorization server.
* Retrieve token info (client_id...) and user info (mail, first/last name..) from access token using Mitreid endpoints.
* Put these token and user info into a configurable (inproc by default) cache for the lifetime of the token (TTL set by Mitreid), thus limiting further Mitreid calls.
* Reject (401) requests that lacks, or does not have a valid access token - except for opt-out [unprotectedpaths] operations.
* Reject (401) API calls that does not fulfil your required oauth2 _scopes_ .
 
This module supports the following oauth2 flows:
 
* _implicit_
* _resource owner password_
* _client credentials_
* _authorization code_
 
# How to use this module?
 
## Subscription process
 
To let Mitreid secure your api, you'll first need a **2-minutes** setup using **Mitreid web app** or **Mitreid [Server API](https://github.com/mitreid-connect/OpenID-Connect-Java-Spring-Server/wiki/API)**.
 
- **Register your api** into Mitreid (including its required scopes)
 
- Ask your **client** (the calling app and/or service) to **Register a client (client_id)** with the needed socpes defined by the registered API.
  These client_ids represent the caller app identity and are reusable (no need to create a client_id per service they need to call)
 
## Maven dependencies
 
You just have to add the dependency to your pom.xml:

```xml
   
    <dependency>
        <groupId>com.github.ezsecure</groupId>
        <artifactId>oauth2-security-starter</artifactId>
        <version>1.0-SNAPSHOT</version>
   </dependency>

```
 
## Configuration
 
By default, all the HTTP operations of your service are secured with a list of scopes that you must define by adding
the property `auth.service.scopes`. You also need to provide the following properties:
 
* `auth.server.reource-id.introspect`:  URL of the access token validation endpoint
* `auth.server.resource-secret`:  URL of the access token validation endpoint
* `auth.server.authorization-server-endpoints.introspect`:  URL of the access token validation endpoint
* `auth.server.authorization-server-endpoints.userinfo`. URL of the user info endpoint
 
Doing this way, all the incoming HTTP requests will be filtered. If you want to setup some unprotected URL, you
have to explicitly list all of them using the property `auth.service.unprotectedpaths`.
 
You might also add more security on some HTTP operation as described on the following sections.
 
## Access restriction using functional oauth2 scopes
 
You have to add the annotation `@PreAuthorize` with the scope requested on the operation, like the following example:
 
```java
 
    @RequestMapping(method = DELETE, value = "{name}")
    @PreAuthorize("#oauth2.hasScope('api.company-service.delete')")
    public ResponseEntity deleteOneCompany(@PathVariable("name") String name) {
        boolean removed = companies.removeIf(company -> company.getName().equals(name));
        if (removed) {
            return noContent().build();
        } else {
            return notFound().build();
        }
    }
```
 
In this snippet, you also need the scope `api.company-service.delete` (to be added in Mitreid for your client ID) in order to delete a company.
 
## Adding Custom Headers
There may be times we wish to inject custom security headers into our application that are not supported out of the box.
For example, we wish to have early support for [Content Security Policy](https://www.w3.org/TR/CSP/) in order to ensure that resources are only loaded from the same origin. Since support for Content Security Policy has not been finalized,
browsers use one of two common extension headers to implement the feature.
This means we will need to inject the policy twice. An example of the headers can be seen below:
 
```yaml
X-Content-Security-Policy: default-src 'self'
X-WebKit-CSP: default-src 'self'
```
 
To do so, we provided a hooks to enable adding custom headers. we have to add a Yaml configuration as follow :
```yaml
    auth:
      server:
        resource-id : THE_API_RESOURCE_ID
        resource-secret : THE_API_RESOURCE_SECRET
        authorization-server-endpoints:
          introspect: http://localhost:8080/openid-connect-server-webapp/introspect
          userinfo: http://localhost:8080/openid-connect-server-webapp/userinfo
      service:
        scopes:
        - api.company-service.v1
        custom-headers:
        -
          header-name: Access-Control-Expose-Headers
          header-value: X-Total-Count
```
 
The configuration is located under the property "custom-headers". we might provide list of header name/header value
