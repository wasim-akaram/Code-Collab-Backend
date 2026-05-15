/**
 * Adds H2 test dependency to all service pom.xml files that don't already have it.
 * Creates test application.yml for each service.
 */
const fs = require('fs');
const path = require('path');

const backendDir = 'd:/CODE-COLLAB/backend';
const services = [
  'project-service', 'file-service', 'collab-service', 'comment-service',
  'execution-service', 'version-service', 'notification-service'
];

// Service-specific base packages
const pkgMap = {
  'project-service': 'project',
  'file-service': 'file',
  'collab-service': 'collab',
  'comment-service': 'comment',
  'execution-service': 'execution',
  'version-service': 'version',
  'notification-service': 'notification'
};

services.forEach(svc => {
  const pomPath = path.join(backendDir, svc, 'pom.xml');
  let pom = fs.readFileSync(pomPath, 'utf8');

  if (!pom.includes('h2database')) {
    pom = pom.replace('</dependencies>',
      `        <dependency>
            <groupId>com.h2database</groupId>
            <artifactId>h2</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>`);
    fs.writeFileSync(pomPath, pom);
    console.log('Added H2 to: ' + svc);
  }

  // Create test resources directory and application.yml
  const testResDir = path.join(backendDir, svc, 'src/test/resources');
  fs.mkdirSync(testResDir, { recursive: true });

  const testYml = `# Test configuration
spring:
  main:
    allow-bean-definition-overriding: true
  datasource:
    url: jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1
    driver-class-name: org.h2.Driver
    username: sa
    password:
  jpa:
    hibernate:
      ddl-auto: create-drop
    database-platform: org.hibernate.dialect.H2Dialect
  cloud:
    discovery:
      enabled: false
  mail:
    host: localhost
    port: 25
    username: test
    password: test

jwt:
  secret: test-jwt-secret-key-for-unit-testing-only-must-be-long

eureka:
  client:
    enabled: false

piston:
  api:
    url: http://localhost:2000
`;

  fs.writeFileSync(path.join(testResDir, 'application.yml'), testYml);
  console.log('Created test config: ' + svc);
});

console.log('\nDone!');
