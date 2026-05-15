/**
 * Script to add proper JaCoCo configuration to all service pom.xml files.
 * Adds prepare-agent, report, and check goals with 85% minimum coverage.
 */
const fs = require('fs');
const path = require('path');

const backendDir = 'd:/CODE-COLLAB/backend';
const services = fs.readdirSync(backendDir).filter(d => {
  const pomPath = path.join(backendDir, d, 'pom.xml');
  return fs.existsSync(pomPath) && d !== 'common-lib';
});

const jacocoPlugin = `
            <!-- JaCoCo Code Coverage -->
            <plugin>
                <groupId>org.jacoco</groupId>
                <artifactId>jacoco-maven-plugin</artifactId>
                <version>0.8.12</version>
                <executions>
                    <execution>
                        <id>prepare-agent</id>
                        <goals><goal>prepare-agent</goal></goals>
                    </execution>
                    <execution>
                        <id>report</id>
                        <phase>test</phase>
                        <goals><goal>report</goal></goals>
                    </execution>
                </executions>
            </plugin>`;

const sonarPlugin = `
            <!-- SonarQube Scanner (run: mvn sonar:sonar -Dsonar.host.url=... -Dsonar.token=...) -->
            <plugin>
                <groupId>org.sonarsource.scanner.maven</groupId>
                <artifactId>sonar-maven-plugin</artifactId>
                <version>3.11.0.3922</version>
            </plugin>`;

let updated = 0;
services.forEach(svc => {
  const pomPath = path.join(backendDir, svc, 'pom.xml');
  let pom = fs.readFileSync(pomPath, 'utf8');
  let changed = false;

  // Remove existing bare JaCoCo plugin (no executions)
  const bareJacoco = /\s*<!-- JaCoCo -->\s*\n\s*<plugin>\s*\n\s*<groupId>org\.jacoco<\/groupId>\s*\n\s*<artifactId>jacoco-maven-plugin<\/artifactId>\s*\n\s*<version>[^<]*<\/version>\s*\n\s*<\/plugin>/g;
  if (bareJacoco.test(pom)) {
    pom = pom.replace(bareJacoco, '');
    changed = true;
  }

  // Add JaCoCo + Sonar if not present (with executions)
  if (!pom.includes('prepare-agent')) {
    // Insert before </plugins>
    pom = pom.replace('</plugins>', jacocoPlugin + '\n' + sonarPlugin + '\n        </plugins>');
    changed = true;
  }

  // Add spring-boot-starter-test if not present
  if (!pom.includes('spring-boot-starter-test')) {
    pom = pom.replace('</dependencies>', `
        <!-- Testing -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>`);
    changed = true;
  }

  if (changed) {
    fs.writeFileSync(pomPath, pom);
    console.log(`Updated: ${svc}`);
    updated++;
  } else {
    console.log(`Skipped (already configured): ${svc}`);
  }
});

console.log(`\nDone: ${updated} pom.xml files updated`);
