# Changelog

## [6.1.39](https://github.com/CenturyLinkCloud/mdw/tree/6.1.39) (2020-10-29)

[Full Changelog](https://github.com/CenturyLinkCloud/mdw/compare/6.1.38...6.1.39)

**Implemented enhancements:**

- changing the header of the java code files to remove centurylink [\#862](https://github.com/CenturyLinkCloud/mdw/issues/862)
- Rebranding from centurylink to Lumen [\#861](https://github.com/CenturyLinkCloud/mdw/issues/861)


## [6.1.38](https://github.com/CenturyLinkCloud/mdw/tree/6.1.38) (2020-08-19)

[Full Changelog](https://github.com/CenturyLinkCloud/mdw/compare/6.1.37...6.1.38)

**Closed issues:**

- MDW will not start on clean environment needing to clone Git repo [\#856](https://github.com/CenturyLinkCloud/mdw/issues/856)

## [6.1.37](https://github.com/CenturyLinkCloud/mdw/tree/6.1.37) (2020-08-07)

[Full Changelog](https://github.com/CenturyLinkCloud/mdw/compare/6.1.36...6.1.37)

**Implemented enhancements:**

- Support for MongoDB 4.x [\#845](https://github.com/CenturyLinkCloud/mdw/issues/845)
- Gson variable type for JSON content [\#451](https://github.com/CenturyLinkCloud/mdw/issues/451)

**Closed issues:**

- Automated tests misc issues [\#853](https://github.com/CenturyLinkCloud/mdw/issues/853)
- When stubbing, sometimes message sent from client to stub server is missing last few characters [\#850](https://github.com/CenturyLinkCloud/mdw/issues/850)
- Automated test expected/actual YAML files cannot be parsed by Groovy if larger than 65k characters [\#849](https://github.com/CenturyLinkCloud/mdw/issues/849)
- Unit testing using MockRuntimeContext is broken [\#846](https://github.com/CenturyLinkCloud/mdw/issues/846)

## [6.1.36](https://github.com/CenturyLinkCloud/mdw/tree/6.1.36) (2020-06-05)

[Full Changelog](https://github.com/CenturyLinkCloud/mdw/compare/6.1.35...6.1.36)

**Implemented enhancements:**

- Annotation driven customization [\#842](https://github.com/CenturyLinkCloud/mdw/issues/842)

**Closed issues:**

- Dangling autotests due to server shutdown [\#844](https://github.com/CenturyLinkCloud/mdw/issues/844)

**Compatibility Notes:**

- New database column VARIABLE_INSTANCE.VARIABLE_TYPE is added with this release:
  https://github.com/CenturyLinkCloud/mdw/tree/master/mdw/database
- JSON-format package meta files (.mdw/package.json) are no longer supported.
  Apps still using package.json must convert to package.yaml using the CLI (`mdw convert --packages`).
- Classes in package `com.centurylink.mdw.connector.adapter` have been moved `com.centurylink.mdw.adapter`.
- Model class `com.centurylink.mdw.model.Response` has been moved to package `com.centurylink.mdw.model.request`.
- Class `com.centurylink.mdw.cache.impl.PackageCache` has been moved to package `com.centurylink.mdw.cache.asset`.
- Method `Package.getCloudClassLoader()` has been renamed to `getClassLoader()`.
- Static method `ApplicationContext.getContextCloudClassLoader()` has been renamed to `getContextPackageClassLoader`.
- Constructor for `com.centurylink.mdw.event.EventHandlerException` no longer takes a code.
- API methods `WorkflowServices.invokeServiceProcess()` and `ProcessEngineDriver.invokeServiceProcess()`
  now return a Response model object instead of a plain string.  To unwrap the raw string payload,
  use `Response.getContent()`.

## [6.1.35](https://github.com/CenturyLinkCloud/mdw/tree/6.1.35) (2020-05-14)

[Full Changelog](https://github.com/CenturyLinkCloud/mdw/compare/6.1.34...6.1.35)

**Implemented enhancements:**

- Replace groovy-all dependency with individual libs [\#841](https://github.com/CenturyLinkCloud/mdw/issues/841)
- Python script activity [\#834](https://github.com/CenturyLinkCloud/mdw/issues/834)
- Use Git history directly instead of ASSET\_REF for inflights [\#816](https://github.com/CenturyLinkCloud/mdw/issues/816)

## [6.1.34](https://github.com/CenturyLinkCloud/mdw/tree/6.1.34) (2020-05-01)

[Full Changelog](https://github.com/CenturyLinkCloud/mdw/compare/6.1.33...6.1.34)

**Implemented enhancements:**

- Close action for manual tasks [\#840](https://github.com/CenturyLinkCloud/mdw/issues/840)
- Activity logging when an instance is skipped or retried [\#839](https://github.com/CenturyLinkCloud/mdw/issues/839)
- Ability to enable activity timings globally [\#838](https://github.com/CenturyLinkCloud/mdw/issues/838)
- Default values for READ\_TIMEOUT and CONNECT\_TIMEOUT in HTTP adapters [\#828](https://github.com/CenturyLinkCloud/mdw/issues/828)

**Closed issues:**

- Activity logging persists some debug messages regardless of configured level [\#837](https://github.com/CenturyLinkCloud/mdw/issues/837)
- Workflow dates are not displayed correctly in Safari [\#836](https://github.com/CenturyLinkCloud/mdw/issues/836)
- Dashboard and milestones compatibility with mdw-mobile [\#835](https://github.com/CenturyLinkCloud/mdw/issues/835)

## [6.1.33](https://github.com/CenturyLinkCloud/mdw/tree/6.1.33) (2020-04-03)

[Full Changelog](https://github.com/CenturyLinkCloud/mdw/compare/6.1.32...6.1.33)

**Implemented enhancements:**

- Ability to disable Git fetch through configuration [\#826](https://github.com/CenturyLinkCloud/mdw/issues/826)
- Improved system thread dump information [\#820](https://github.com/CenturyLinkCloud/mdw/issues/820)
- Include selected filter information when exporting to Excel from Hub [\#819](https://github.com/CenturyLinkCloud/mdw/issues/819)
- Indicate when Hub filters are active \(set to non-defaults\) [\#817](https://github.com/CenturyLinkCloud/mdw/issues/817)
- Export Process definition from Hub [\#810](https://github.com/CenturyLinkCloud/mdw/issues/810)
- Ability to override global Activity Logging enablement [\#799](https://github.com/CenturyLinkCloud/mdw/issues/799)
- Dashboard chart filter end date selection for non-current month [\#684](https://github.com/CenturyLinkCloud/mdw/issues/684)
- Remember dashboard filter/breakdown user selections [\#584](https://github.com/CenturyLinkCloud/mdw/issues/584)

**Closed issues:**

- Multiline Script and dynamic Java attributes are not folded in YAML .proc files [\#827](https://github.com/CenturyLinkCloud/mdw/issues/827)
- Content-based event handler registrations with 'Topic' metaInfo are broken [\#823](https://github.com/CenturyLinkCloud/mdw/issues/823)
- In MDWHub milestones definition subflow drill-in can cause HTTP 404 [\#821](https://github.com/CenturyLinkCloud/mdw/issues/821)
- Milestone labels without group fails to unescape newline characters [\#818](https://github.com/CenturyLinkCloud/mdw/issues/818)
- CLI process export to HTML/PDF fails to display icons for built-in activities [\#800](https://github.com/CenturyLinkCloud/mdw/issues/800)

## [6.1.32](https://github.com/CenturyLinkCloud/mdw/tree/6.1.32) (2020-03-06)

[Full Changelog](https://github.com/CenturyLinkCloud/mdw/compare/6.1.31...6.1.32)

**Implemented enhancements:**

- Monitors for inflight instances should use latest process definition [\#814](https://github.com/CenturyLinkCloud/mdw/issues/814)
- General purpose Configurator help links in MDWHub [\#804](https://github.com/CenturyLinkCloud/mdw/issues/804)
- Package dependencies [\#395](https://github.com/CenturyLinkCloud/mdw/issues/395)

**Closed issues:**

- Package dependency check fails when Git dir is not cwd [\#815](https://github.com/CenturyLinkCloud/mdw/issues/815)
- Boot jar startup failure on Windows [\#813](https://github.com/CenturyLinkCloud/mdw/issues/813)
- Hub milestones not populated unless Milestones link visited [\#812](https://github.com/CenturyLinkCloud/mdw/issues/812)
- Adapter completion time update error at performance level 5 [\#809](https://github.com/CenturyLinkCloud/mdw/issues/809)
- CLI without command throws IndexOutOfBoundsException on Windows [\#807](https://github.com/CenturyLinkCloud/mdw/issues/807)
- Hub zoom controls can overlay Inspector tab [\#795](https://github.com/CenturyLinkCloud/mdw/issues/795)

## [6.1.31](https://github.com/CenturyLinkCloud/mdw/tree/6.1.31) (2020-02-14)

[Full Changelog](https://github.com/CenturyLinkCloud/mdw/compare/6.1.30...6.1.31)

**Implemented enhancements:**

- Package dependencies [\#395](https://github.com/CenturyLinkCloud/mdw/issues/395)
- App Version should be read from Spring Boot jar manifest [\#803](https://github.com/CenturyLinkCloud/mdw/issues/803)
- Support package.yaml files with -SNAPSHOT versions [\#531](https://github.com/CenturyLinkCloud/mdw/issues/531)

**Closed issues:**

- Overlong activity log messages cause runtime SQLException [\#802](https://github.com/CenturyLinkCloud/mdw/issues/802)
- Export to PDF does not include activity markdown documentation [\#801](https://github.com/CenturyLinkCloud/mdw/issues/801)
- GitLab asset discovery limited by default per_page parameter value [\#794](https://github.com/CenturyLinkCloud/mdw/issues/794)
- Broken CI due to Maven Central HTTPS requirement [\#793](https://github.com/CenturyLinkCloud/mdw/issues/793)
- Dashboard Process Insights by Month is broken [\#765](https://github.com/CenturyLinkCloud/mdw/issues/765)
- CLI dependencies failure with OpenJDK 11 [\#728](https://github.com/CenturyLinkCloud/mdw/issues/728)

## [6.1.30](https://github.com/CenturyLinkCloud/mdw/tree/6.1.30) (2020-01-10)

[Full Changelog](https://github.com/CenturyLinkCloud/mdw/compare/6.1.29...6.1.30)

**Implemented enhancements:**

- DependenciesFallbackPublish should handle pre-existing EVENT\_INSTANCE [\#791](https://github.com/CenturyLinkCloud/mdw/issues/791)
- ServiceNow adapter activity [\#781](https://github.com/CenturyLinkCloud/mdw/issues/781)
- Add primary key to ACTIVITY\_LOG table for Oracle [\#776](https://github.com/CenturyLinkCloud/mdw/issues/776)

**Closed issues:**

- Corruption in 6.1.29 asset zip files on Maven Central [\#790](https://github.com/CenturyLinkCloud/mdw/issues/790)
- Unparseable adapter response content can prevent activity retry [\#784](https://github.com/CenturyLinkCloud/mdw/issues/784)
- Annotated ProcessCleanup should not honor old property values [\#779](https://github.com/CenturyLinkCloud/mdw/issues/779)

**Compatibility Notes:**

- To avoid `Error: zip END header not found​` when updating to 6.1.30 assets, install the latest
  [CLI](https://centurylinkcloud.github.io/mdw/docs/getting-started/cli/)
  and/or [MDW Studio](https://centurylinkcloud.github.io/mdw/docs/guides/mdw-studio/) version 2.0.2 or later.

## [6.1.29](https://github.com/CenturyLinkCloud/mdw/tree/6.1.29) (2019-12-13)

[Full Changelog](https://github.com/CenturyLinkCloud/mdw/compare/6.1.28...6.1.29)

**Implemented enhancements:**

- Upgrade Groovy dependency to 2.5.8 [\#787](https://github.com/CenturyLinkCloud/mdw/issues/787)
- Engine should not assume process start activity's logical ID is A1 [\#786](https://github.com/CenturyLinkCloud/mdw/issues/786)
- Change default transaction isolation level to READ_COMMITTED for MySQL/MariaDB [\#780](https://github.com/CenturyLinkCloud/mdw/issues/780)
- Scheduled fallback processing for Wait Activities [\#778](https://github.com/CenturyLinkCloud/mdw/issues/778)
- Zoom workflow canvas in Hub [\#775](https://github.com/CenturyLinkCloud/mdw/issues/775)
- Upgrade MariaDB driver dependency [\#772](https://github.com/CenturyLinkCloud/mdw/issues/772)
- Convert StuckActivities Scheduled Job to annotated format [\#769](https://github.com/CenturyLinkCloud/mdw/issues/769)
- Adapter retry should count failed instances since last success [\#722](https://github.com/CenturyLinkCloud/mdw/issues/722)

**Closed issues:**

- Engine should avoid NullPointerException when process definition is not found [\#789](https://github.com/CenturyLinkCloud/mdw/issues/789)
- Adapter request Jsonables not unwrapped before invoke  [\#783](https://github.com/CenturyLinkCloud/mdw/issues/783)
- Configurator Events tab not displayed for some activities [\#777](https://github.com/CenturyLinkCloud/mdw/issues/777)
- Hub should save ClassName attribute for dynamic Java activities [\#699](https://github.com/CenturyLinkCloud/mdw/issues/699)

**Compatibility Notes:**

- The Groovy upgrade (issue [\#787](https://github.com/CenturyLinkCloud/mdw/issues/787)) exposes an issue with
  Maven transitive dependency resolution due to POM repackaging of groovy-all-2.5.x.  Gradle builds are not affected, but if
  you're using Maven you'll need to institute a workaround to avoid "Failure to find org.codehaus.groovy:groovy-all:jar:2.5.8".
  This involves excluding groovy-all as a transitive dependency via MDW, and instead declaring groovy-all as a direct dependency
  in your pom.xml:
  ```xml
      <dependency>
          <groupId>com.centurylink.mdw</groupId>
          <artifactId>mdw-spring-boot</artifactId>
          <version>6.1.30</version>
          <exclusions>
              <exclusion>
                  <groupId>org.codehaus.groovy</groupId>
                  <artifactId>groovy-all</artifactId>
              </exclusion>
          </exclusions>
      </dependency>
      <dependency>
          <groupId>org.codehaus.groovy</groupId>
          <artifactId>groovy-all</artifactId>
          <version>2.5.8</version>
          <type>pom</type>
      </dependency>
  ```

## [6.1.28](https://github.com/CenturyLinkCloud/mdw/tree/6.1.28) (2019-11-22)

[Full Changelog](https://github.com/CenturyLinkCloud/mdw/compare/6.1.27...6.1.28)

**Implemented enhancements:**

- MDWHub asset discoveryType should always be Git [\#761](https://github.com/CenturyLinkCloud/mdw/issues/761)
- Include runtime instance info in all activity logger output [\#758](https://github.com/CenturyLinkCloud/mdw/issues/758)
- Scheduled Job for log rotation [\#182](https://github.com/CenturyLinkCloud/mdw/issues/182)

**Closed issues:**

- Page refresh required to display Inspector Subprocesses tab [\#773](https://github.com/CenturyLinkCloud/mdw/issues/773)
- MySQL transaction isolation level may fail to be reset [\#771](https://github.com/CenturyLinkCloud/mdw/issues/771)
- Annotated ScheduledJobs enablement should honor defaultEnabled [\#770](https://github.com/CenturyLinkCloud/mdw/issues/770)
- Stuck processes due to server shutdown [\#736](https://github.com/CenturyLinkCloud/mdw/issues/736)

**Compatibility Notes:**

  - For #758 Activity logging, in-place db upgrade automatically adds the new ACTIVITY_LOG table.  If your db app user
    lacks permission, you'll need to create the table by executing the steps at the bottom of the upgrade SQL scripts:
     - https://github.com/CenturyLinkCloud/mdw/blob/master/mdw/database/mysql/mdw_upgrade_6.0_To_6.1.sql
     - https://github.com/CenturyLinkCloud/mdw/blob/master/mdw/database/oracle/mdw_upgrade_6.0_To_6.1.sql

## [6.1.27](https://github.com/CenturyLinkCloud/mdw/tree/6.1.27) (2019-11-08)

[Full Changelog](https://github.com/CenturyLinkCloud/mdw/compare/6.1.26...6.1.27)

**Implemented enhancements:**

- Milestones API should allow specifying a master process instance [\#757](https://github.com/CenturyLinkCloud/mdw/issues/757)
- Asset modifications staging [\#720](https://github.com/CenturyLinkCloud/mdw/issues/720)
  https://github.com/CenturyLinkCloud/mdw/blob/master/mdw-workflow/assets/com/centurylink/mdw/staging/readme.md

**Closed issues:**

- Avoid selecting documents for update when notifying ActivityMonitors [\#767](https://github.com/CenturyLinkCloud/mdw/issues/767)
- REST adapter activity should populate response variable even for non-2xx [\#762](https://github.com/CenturyLinkCloud/mdw/issues/762)
- Event name existence check SQLException catch is too general [\#760](https://github.com/CenturyLinkCloud/mdw/issues/760)
- SQLException can be swallowed in ProcessEngineDriver.processEvents\(\) [\#759](https://github.com/CenturyLinkCloud/mdw/issues/759)
- Cancelling a flow through Hub should cancel tasks/waits in embedded handlers [\#754](https://github.com/CenturyLinkCloud/mdw/issues/754)
- Cancel/Complete error task should not retrigger already-proceeded flow [\#744](https://github.com/CenturyLinkCloud/mdw/issues/744)

## [6.1.26](https://github.com/CenturyLinkCloud/mdw/tree/6.1.26) (2019-10-04)

[Full Changelog](https://github.com/CenturyLinkCloud/mdw/compare/6.1.25...6.1.26)

**Implemented enhancements:**

- Hub search should be case-insensitive [\#752](https://github.com/CenturyLinkCloud/mdw/issues/752)
- Broken search by task name on MDWHub's Tasks tab [\#751](https://github.com/CenturyLinkCloud/mdw/issues/751)
- Convert base activities impls to annotation form [\#748](https://github.com/CenturyLinkCloud/mdw/issues/748)
- Set response Content-Length for REST services [\#742](https://github.com/CenturyLinkCloud/mdw/issues/742)
- Avoid persisting Hub REST requests/responses [\#738](https://github.com/CenturyLinkCloud/mdw/issues/738)
- Show process version history in MDWHub [\#677](https://github.com/CenturyLinkCloud/mdw/issues/677)
- Non-asset Java classes should support the @Activity annotation. [\#31](https://github.com/CenturyLinkCloud/mdw/issues/31)

**Closed issues:**

- Manual task retry in package-level error handlers. [\#750](https://github.com/CenturyLinkCloud/mdw/issues/750)
- Editing activity monitors in MDWHub is broken [\#749](https://github.com/CenturyLinkCloud/mdw/issues/749)
- MDW CLI convert command missing slf4j dependency [\#737](https://github.com/CenturyLinkCloud/mdw/issues/737)
- Spring assets with JAXB on Kubernetes with OpenJDK 11 [\#717](https://github.com/CenturyLinkCloud/mdw/issues/717)
- Package-level handler processes not canceled with owning process [\#710](https://github.com/CenturyLinkCloud/mdw/issues/710)
- ConcurrentModificationException checking Hub path [\#700](https://github.com/CenturyLinkCloud/mdw/issues/700)
- Editing process instance always load latest definition instead of instance's definition [\#676](https://github.com/CenturyLinkCloud/mdw/issues/676)
- Cannot Cancel/Abort Tasks from Tasks list due to missing Comment [\#637](https://github.com/CenturyLinkCloud/mdw/issues/637)

**Compatibility Notes:**
  - Due to [\#748](https://github.com/CenturyLinkCloud/mdw/issues/748), MDW Studio 1.3.6+ is required for 6.1.26.
    Without upgrading to 1.3.6+, base activities will be missing from Studio's Toolbox view.

## [6.1.25](https://github.com/CenturyLinkCloud/mdw/tree/6.1.25) (2019-09-06)

[Full Changelog](https://github.com/CenturyLinkCloud/mdw/compare/6.1.24...6.1.25)

**Implemented enhancements:**

- Notify registered TaskMonitors of lifecycle changes [\#517](https://github.com/CenturyLinkCloud/mdw/issues/517)

**Closed issues:**

- Inflight instance definitions not parseable [\#740](https://github.com/CenturyLinkCloud/mdw/issues/740)
- Manual task activities with invalid taskInstanceId expression [\#739](https://github.com/CenturyLinkCloud/mdw/issues/739)
- Excel export fails for instance lists in MDWHub [\#734](https://github.com/CenturyLinkCloud/mdw/issues/734)

## [6.1.24](https://github.com/CenturyLinkCloud/mdw/tree/6.1.24) (2019-08-21)

[Full Changelog](https://github.com/CenturyLinkCloud/mdw/compare/6.1.23...6.1.24)

**Closed issues:**

- Milestone StackOverflowErrors due to duplication in activity/instance hierarchy [\#730](https://github.com/CenturyLinkCloud/mdw/issues/730)

## [6.1.23](https://github.com/CenturyLinkCloud/mdw/tree/6.1.23) (2019-08-10)

[Full Changelog](https://github.com/CenturyLinkCloud/mdw/compare/6.1.22...6.1.23)

**Implemented enhancements:**

- Milestone labels should support runtime expressions [\#725](https://github.com/CenturyLinkCloud/mdw/issues/725)
- Upgrade React and provide reproducibility for node\_modules.zip [\#719](https://github.com/CenturyLinkCloud/mdw/issues/719)
- Allow expressions for transition delay and maxRetries attributes [\#715](https://github.com/CenturyLinkCloud/mdw/issues/715)
- Support yaml-format .proc assets [\#706](https://github.com/CenturyLinkCloud/mdw/issues/706)
- Javadocs improvements [\#705](https://github.com/CenturyLinkCloud/mdw/issues/705)

**Closed issues:**

- Cannot save process instance definition on MySQL without lower\_case\_table\_names [\#729](https://github.com/CenturyLinkCloud/mdw/issues/729)
- Milestones for multiple instances of same subflow [\#723](https://github.com/CenturyLinkCloud/mdw/issues/723)
- Context root is hardcoded in some Task JSX assets [\#718](https://github.com/CenturyLinkCloud/mdw/issues/718)
- Full support for dynamic java activity code [\#714](https://github.com/CenturyLinkCloud/mdw/issues/714)
- Swagger header params incorrectly appended to path [\#713](https://github.com/CenturyLinkCloud/mdw/issues/713)
- Cannot resolve Spring XSDs for dynamic .spring assets when disconnected from the internet [\#712](https://github.com/CenturyLinkCloud/mdw/issues/712)
- Subprocesses Inspector tab for Microservice Orchestrator shows misleading start time [\#666](https://github.com/CenturyLinkCloud/mdw/issues/666)

**Compatibility Notes:**
  - MDW Designer is no longer supported from 6.1.23.  Please use [MDW Studio](https://centurylinkcloud.github.io/mdw/docs/guides/mdw-studio/) instead.
    Reasons for this are outlined [in the documentation](https://centurylinkcloud.github.io/mdw/docs/designer/designer-support/).

## [6.1.22](https://github.com/CenturyLinkCloud/mdw/tree/6.1.22) (2019-07-19)

[Full Changelog](https://github.com/CenturyLinkCloud/mdw/compare/6.1.21...6.1.22)

**Implemented enhancements:**

- Swagger retrieval by path should support classes with parameterized segments [\#708](https://github.com/CenturyLinkCloud/mdw/issues/708)
- Ability to skip comparison for specified variables in autotest expected yaml [\#701](https://github.com/CenturyLinkCloud/mdw/issues/701)
- Option to avoid formatting MongoDB JSON document content [\#693](https://github.com/CenturyLinkCloud/mdw/issues/693)

**Closed issues:**

- Inefficient query for MySQL in retrieving EVENT\_WAIT\_INSTANCE rows [\#707](https://github.com/CenturyLinkCloud/mdw/issues/707)
- RestServiceAdapter content type defaulted incorrectly [\#702](https://github.com/CenturyLinkCloud/mdw/issues/702)
- Some standalone Tomcat deployments fail to load Hub's login and index pages [\#698](https://github.com/CenturyLinkCloud/mdw/issues/698)
- In Hub process instance Values nav link, output variables cannot be edited [\#697](https://github.com/CenturyLinkCloud/mdw/issues/697)
- Hub process instance Cancel button not functional [\#696](https://github.com/CenturyLinkCloud/mdw/issues/696)
- Startup race condition in TaskDataAccess results in query with duplicate columns [\#648](https://github.com/CenturyLinkCloud/mdw/issues/648)
- Transition delay value is wrong when entered via Hub [\#631](https://github.com/CenturyLinkCloud/mdw/issues/631)

## [6.1.21](https://github.com/CenturyLinkCloud/mdw/tree/6.1.21) (2019-06-22)

[Full Changelog](https://github.com/CenturyLinkCloud/mdw/compare/6.1.20...6.1.21)

**Implemented enhancements:**

- Allow path expressions for SwaggerValidatorActivity [\#688](https://github.com/CenturyLinkCloud/mdw/issues/688)
- Microsoft Teams webhook notifications [\#672](https://github.com/CenturyLinkCloud/mdw/issues/672)
- Support Java 11 [\#358](https://github.com/CenturyLinkCloud/mdw/issues/358)

**Closed issues:**

- Dashboard charts inaccurate when server time differs from browser time [\#694](https://github.com/CenturyLinkCloud/mdw/issues/694)
- Milestones and Traversed sequence not displayed for previous subprocess versions [\#691](https://github.com/CenturyLinkCloud/mdw/issues/691)
- Include milestones/Main.jsx in React asset precompilation [\#690](https://github.com/CenturyLinkCloud/mdw/issues/690)

## [6.1.20](https://github.com/CenturyLinkCloud/mdw/tree/6.1.20) (2019-06-08)

[Full Changelog](https://github.com/CenturyLinkCloud/mdw/compare/6.1.19...6.1.20)

**Implemented enhancements:**

- AssetImportMonitor disabled by default in development [\#680](https://github.com/CenturyLinkCloud/mdw/issues/680)
- Daily/Hourly timespan option for Dashboard [\#668](https://github.com/CenturyLinkCloud/mdw/issues/668)
- Ugly exception stack traces due to WebSocket timeouts [\#667](https://github.com/CenturyLinkCloud/mdw/issues/667)
- History retention for System Monitor dashboard tracking [\#665](https://github.com/CenturyLinkCloud/mdw/issues/665)
- Enable db cleanup scheduled job by default [\#661](https://github.com/CenturyLinkCloud/mdw/issues/661)
- Workflow milestones [\#652](https://github.com/CenturyLinkCloud/mdw/issues/652)

**Closed issues:**

- Null values in adapter requestHeaders prevent meta data persistence [\#682](https://github.com/CenturyLinkCloud/mdw/issues/682)
- Adapter activity auto-retry is broken [\#678](https://github.com/CenturyLinkCloud/mdw/issues/678)
- Dashboard NumberFormatExceptions when Sample changed with no Process selected [\#675](https://github.com/CenturyLinkCloud/mdw/issues/675)
- REST service assets may be incorrectly resolved according to shortest package path [\#674](https://github.com/CenturyLinkCloud/mdw/issues/674)
- Process count in Dashboard chart can differ from that shown in process list [\#673](https://github.com/CenturyLinkCloud/mdw/issues/673)

## [6.1.19](https://github.com/CenturyLinkCloud/mdw/tree/6.1.19) (2019-05-23)

[Full Changelog](https://github.com/CenturyLinkCloud/mdw/compare/6.1.18...6.1.19)

**Closed issues:**

- Request tab is broken for inbound requests [\#670](https://github.com/CenturyLinkCloud/mdw/issues/670)
- AssetImportMonitor Vercheck exception [\#669](https://github.com/CenturyLinkCloud/mdw/issues/669)

## [6.1.18](https://github.com/CenturyLinkCloud/mdw/tree/6.1.18) (2019-05-17)

[Full Changelog](https://github.com/CenturyLinkCloud/mdw/compare/6.1.17...6.1.18)

**Closed issues:**

- Vercheck issues when invoked programmatically [\#664](https://github.com/CenturyLinkCloud/mdw/issues/664)
- Fix KafkaAdapter initialization [\#663](https://github.com/CenturyLinkCloud/mdw/issues/663)
- REST annotations should prefer qualified paths over root [\#662](https://github.com/CenturyLinkCloud/mdw/issues/662)
- Support for in-flight dynamic java activity java code [\#641](https://github.com/CenturyLinkCloud/mdw/issues/641)

## [6.1.17](https://github.com/CenturyLinkCloud/mdw/tree/6.1.17) (2019-05-10)

[Full Changelog](https://github.com/CenturyLinkCloud/mdw/compare/6.1.16...6.1.17)

**Compatibility Notes:**
  - Class com.centurylink.mdw.util.StringHelper is deprecated.  See javadocs for alternatives.
  - Methods com.centurylink.mdw.model.attribute.Attribute.getAttributeName()/getAttributeValue() are replaced by getName()/getValue().
  - Override attributes are no longer supported.

**Implemented enhancements:**

- ASSET\_REF auto-population disabled by default in dev mode [\#658](https://github.com/CenturyLinkCloud/mdw/issues/658)
- Process definition hierarchy in MDWHub and CLI [\#651](https://github.com/CenturyLinkCloud/mdw/issues/651)
- CLI DB export/import capability [\#650](https://github.com/CenturyLinkCloud/mdw/issues/650)
- Dashboard charts for live system monitoring [\#646](https://github.com/CenturyLinkCloud/mdw/issues/646)
- Display MBean info on Hub's System tab [\#645](https://github.com/CenturyLinkCloud/mdw/issues/645)
- CLI vercheck should default to only scanning updated assets [\#643](https://github.com/CenturyLinkCloud/mdw/issues/643)
- Option to prevent overlap in Scheduled Job execution [\#642](https://github.com/CenturyLinkCloud/mdw/issues/642)
- Zipkin instrumentation for subflows [\#580](https://github.com/CenturyLinkCloud/mdw/issues/580)

**Closed issues:**

- Dashboard chart drill into requests by path is broken [\#656](https://github.com/CenturyLinkCloud/mdw/issues/656)
- Subprocess instances from embedded subflow not displayed in Inspector [\#654](https://github.com/CenturyLinkCloud/mdw/issues/654)
- Prevent Hub asset import from failing due to OS newline differences [\#649](https://github.com/CenturyLinkCloud/mdw/issues/649)
- Default Package is not recreated upon cache refresh [\#644](https://github.com/CenturyLinkCloud/mdw/issues/644)
- Activities API returns incorrect total count [\#640](https://github.com/CenturyLinkCloud/mdw/issues/640)
- Vercheck should ignore line-ending diffs for text assets [\#638](https://github.com/CenturyLinkCloud/mdw/issues/638)

## [6.1.16](https://github.com/CenturyLinkCloud/mdw/tree/6.1.16) (2019-03-29)

[Full Changelog](https://github.com/CenturyLinkCloud/mdw/compare/6.1.15...6.1.16)

**Implemented enhancements:**

- Vercheck should default to comparing vs remote branch [\#632](https://github.com/CenturyLinkCloud/mdw/issues/632)
- Preferential loading of asset classes over WAR/Spring Boot JAR classes [\#630](https://github.com/CenturyLinkCloud/mdw/issues/630)
- Ability to manually Fail an activity just like Retry/Proceed [\#629](https://github.com/CenturyLinkCloud/mdw/issues/629)
- Asset package discovery via Git [\#621](https://github.com/CenturyLinkCloud/mdw/issues/621)
- Failure during Process Instance creation should leave instance in Failed status [\#620](https://github.com/CenturyLinkCloud/mdw/issues/620)
- Topic/Path-based routing for Event Handlers [\#250](https://github.com/CenturyLinkCloud/mdw/issues/250)

**Closed issues:**

- Event wait activity does not proceed when timeout is configured via expression [\#636](https://github.com/CenturyLinkCloud/mdw/issues/636)
- Inspector instance hierarchy view incorrect tree structure [\#635](https://github.com/CenturyLinkCloud/mdw/issues/635)
- UserGroupCache can contain users with missing attributes [\#634](https://github.com/CenturyLinkCloud/mdw/issues/634)
- Dashboard query errors with Oracle db [\#627](https://github.com/CenturyLinkCloud/mdw/issues/627)
- Viewing and updating process variables using a manual task in embedded subprocess [\#626](https://github.com/CenturyLinkCloud/mdw/issues/626)
- Dashboard Requests path parameters lack URL encoding [\#624](https://github.com/CenturyLinkCloud/mdw/issues/624)
- List type process variables experience issues whenever they contain null entries [\#623](https://github.com/CenturyLinkCloud/mdw/issues/623)
- User-friendly message for tabs and nav links to missing React assets [\#609](https://github.com/CenturyLinkCloud/mdw/issues/609)

## [6.1.15](https://github.com/CenturyLinkCloud/mdw/tree/6.1.15) (2019-02-21)

[Full Changelog](https://github.com/CenturyLinkCloud/mdw/compare/6.1.14...6.1.15)

**Compatibility Notes:**
  - To take advantage of enhanced dashboard charts introduced by issue #582, it's highly recommended that you add the indexes
    commented out at the bottom of the update scripts:
     - https://github.com/CenturyLinkCloud/mdw/blob/master/mdw/database/mysql/mdw_upgrade_6.0_To_6.1.sql
     - https://github.com/CenturyLinkCloud/mdw/blob/master/mdw/database/oracle/mdw_upgrade_6.0_To_6.1.sql
    Depending on how much existing data is present, these indexes may take a while to create.

**Implemented enhancements:**

- Apply needed MongoDB options whenever not present in specified URI [\#616](https://github.com/CenturyLinkCloud/mdw/issues/616)
- Ability to specify high priority in SMTP emails [\#614](https://github.com/CenturyLinkCloud/mdw/issues/614)
- FilePanel enhancements for Kubernetes [\#611](https://github.com/CenturyLinkCloud/mdw/issues/611)
- Display id and version on process Inspector tab [\#610](https://github.com/CenturyLinkCloud/mdw/issues/610)
- Webpack precompile JSX assets in non-dev environments [\#608](https://github.com/CenturyLinkCloud/mdw/issues/608)
- Implement process monitor for updating Service Summary [\#607](https://github.com/CenturyLinkCloud/mdw/issues/607)
- Support encrypted property values for groups and lists [\#604](https://github.com/CenturyLinkCloud/mdw/issues/604)
- Implement new ScheduledJob to automatically handle stuck activities [\#603](https://github.com/CenturyLinkCloud/mdw/issues/603)
- Process/Request Insights charts in MDWHub dashboard [\#598](https://github.com/CenturyLinkCloud/mdw/issues/598)
- Process Hotspots view in Dashboard tab [\#597](https://github.com/CenturyLinkCloud/mdw/issues/597)
- Microservice dependencies wait activity [\#419](https://github.com/CenturyLinkCloud/mdw/issues/419)

**Closed issues:**

- Asset save action via Hub does not work [\#618](https://github.com/CenturyLinkCloud/mdw/issues/618)
- Hub doesn't display the Output Document lists for Adapter type activities [\#617](https://github.com/CenturyLinkCloud/mdw/issues/617)
- Swagger Validator activity generates no response when OK [\#615](https://github.com/CenturyLinkCloud/mdw/issues/615)
- NullPointerExceptions resulting from issue \#457 [\#613](https://github.com/CenturyLinkCloud/mdw/issues/613)
- FilePanel fixes and enhancements [\#612](https://github.com/CenturyLinkCloud/mdw/issues/612)
- Repetitive loading/logging in ServicePaths cache [\#606](https://github.com/CenturyLinkCloud/mdw/issues/606)
- In Hub Services tab, method-defined subpaths cause a server error when clicked [\#602](https://github.com/CenturyLinkCloud/mdw/issues/602)
- Custom React Index.jsx assets fail to attach to DOM element on Windows [\#601](https://github.com/CenturyLinkCloud/mdw/issues/601)
- Out-of-order dates for new month in dashboard breakdown data [\#599](https://github.com/CenturyLinkCloud/mdw/issues/599)

## [6.1.14](https://github.com/CenturyLinkCloud/mdw/tree/6.1.14) (2019-02-01)

**Compatibility Notes:**
  - If using an embedded db on a case-sensitive file system where you wish to preserve data, add `--lower_case_table_names=1`
    to the db.startup section of mdw.yaml as illustrated in the config guide: https://centurylinkcloud.github.io/mdw/docs/guides/configuration/#mdwyaml.
    This change is due to enhancement issue #466.

[Full Changelog](https://github.com/CenturyLinkCloud/mdw/compare/6.1.13...6.1.14)

**Implemented enhancements:**

- Activities list enhanced search and filtering [\#595](https://github.com/CenturyLinkCloud/mdw/issues/595)
- Update ScheduledJob schedule when annotation changes [\#594](https://github.com/CenturyLinkCloud/mdw/issues/594)
- More comprehensive MongoDB support  [\#593](https://github.com/CenturyLinkCloud/mdw/issues/593)
- Show millisecond date/time precision in MDWHub if available [\#591](https://github.com/CenturyLinkCloud/mdw/issues/591)
- Requests list enhanced search and filtering [\#588](https://github.com/CenturyLinkCloud/mdw/issues/588)
- Show response times in MDWHub for both inbound and outbound requests [\#587](https://github.com/CenturyLinkCloud/mdw/issues/587)
- Dashboard charts revamp [\#582](https://github.com/CenturyLinkCloud/mdw/issues/582)
- Visibility of actual create/due/end date for manual tasks [\#578](https://github.com/CenturyLinkCloud/mdw/issues/578)
- Remove MySQL/MariaDB lower\_case\_table\_names requirement [\#466](https://github.com/CenturyLinkCloud/mdw/issues/466)
- DB trace capability for DBCP connections [\#323](https://github.com/CenturyLinkCloud/mdw/issues/323)

**Closed issues:**

- Service flow autotest waits always time out instead of proactively finishing [\#589](https://github.com/CenturyLinkCloud/mdw/issues/589)
- IllegalStateException due to Spring Boot Mustache autoconfig [\#586](https://github.com/CenturyLinkCloud/mdw/issues/586)
- War artifact not published using new "maven-publish" gradle plugin [\#577](https://github.com/CenturyLinkCloud/mdw/issues/577)
- Linkage errors due to eager classloading for @RegisteredService and other annotations [\#561](https://github.com/CenturyLinkCloud/mdw/issues/561)
- HTTP 500 when querying for Tasks by nonexistent workgroup [\#560](https://github.com/CenturyLinkCloud/mdw/issues/560)

## [6.1.13](https://github.com/CenturyLinkCloud/mdw/tree/6.1.13) (2019-01-12)

[Full Changelog](https://github.com/CenturyLinkCloud/mdw/compare/6.1.11...6.1.13)

**Implemented enhancements:**

- Ability to retrieve and verify MDW Auth tokens using CLI [\#574](https://github.com/CenturyLinkCloud/mdw/issues/574)
- Redundant timestamp in workflow log line  [\#571](https://github.com/CenturyLinkCloud/mdw/issues/571)
- Pagelet content in @Activity annotations can be externalized [\#570](https://github.com/CenturyLinkCloud/mdw/issues/570)
- Support multiple/nested service orchestrations [\#569](https://github.com/CenturyLinkCloud/mdw/issues/569)
- Integration with Status Manager [\#568](https://github.com/CenturyLinkCloud/mdw/issues/568)
- Spring Sleuth and Zipkin Support [\#564](https://github.com/CenturyLinkCloud/mdw/issues/564)
- CLI command to convert .impl file to annotated form [\#562](https://github.com/CenturyLinkCloud/mdw/issues/562)
- Built-in StandardLogger for SLF4J [\#558](https://github.com/CenturyLinkCloud/mdw/issues/558)
- Allow Proxy.Type = HTTP in HttpHelper and REST adapter activity [\#557](https://github.com/CenturyLinkCloud/mdw/issues/557)
- Allow for configuring multiple custom JWT providers [\#556](https://github.com/CenturyLinkCloud/mdw/issues/556)
- Include path info in MDWHub Requests display [\#551](https://github.com/CenturyLinkCloud/mdw/issues/551)
- Instance-level process changes [\#537](https://github.com/CenturyLinkCloud/mdw/issues/537)
- Support for asset import from Git tags as well as branches [\#492](https://github.com/CenturyLinkCloud/mdw/issues/492)
- Enforce uniqueness for Master Request ID [\#468](https://github.com/CenturyLinkCloud/mdw/issues/468)
- Listener and adapter requests created even for empty content [\#457](https://github.com/CenturyLinkCloud/mdw/issues/457)
- Alternative multiserver configuration for FilePanel [\#402](https://github.com/CenturyLinkCloud/mdw/issues/402)
- ScheduledJob via @RegisteredService [\#369](https://github.com/CenturyLinkCloud/mdw/issues/369)

**Closed issues:**

- Activity in Hub shows all subflows launched by all subflow-launching activities from a process [\#576](https://github.com/CenturyLinkCloud/mdw/issues/576)
- Dynamic Java Classpath is missing MDW classes and JARs suddenly  [\#575](https://github.com/CenturyLinkCloud/mdw/issues/575)
- Parsing exceptions due to slf4j logging [\#573](https://github.com/CenturyLinkCloud/mdw/issues/573)
- YamlProperties cannot handle group/map properties with boolean values [\#572](https://github.com/CenturyLinkCloud/mdw/issues/572)
- Process variable instances passed into children instances should not change ownership  [\#567](https://github.com/CenturyLinkCloud/mdw/issues/567)
- Notification activity unable to parse email list [\#566](https://github.com/CenturyLinkCloud/mdw/issues/566)
- MDWHub cannot edit embedded subprocess components of a process  [\#554](https://github.com/CenturyLinkCloud/mdw/issues/554)
- MDWHub cannot properly select activity and task instances within an Embedded sub process [\#553](https://github.com/CenturyLinkCloud/mdw/issues/553)
- mdw-spring-boot-sources.jar should include mdw-hub source files [\#552](https://github.com/CenturyLinkCloud/mdw/issues/552)
- Publishing to Nexus from Travis CI is broken [\#550](https://github.com/CenturyLinkCloud/mdw/issues/550)
- Handle empty but non-null request content in TextAdapterActivity [\#545](https://github.com/CenturyLinkCloud/mdw/issues/545)
- Dashboard charts are broken [\#462](https://github.com/CenturyLinkCloud/mdw/issues/462)

## [6.1.11](https://github.com/CenturyLinkCloud/mdw/tree/6.1.11) (2018-11-02)

[Full Changelog](https://github.com/CenturyLinkCloud/mdw/compare/6.1.09...6.1.11)

**Implemented enhancements:**

- Upgrade to kotlin 1.3 [\#548](https://github.com/CenturyLinkCloud/mdw/issues/548)
- CLI commands to encrypt and decrypt values [\#541](https://github.com/CenturyLinkCloud/mdw/issues/541)
- Encrypted values in mdw.yaml [\#540](https://github.com/CenturyLinkCloud/mdw/issues/540)
- Enhanced mdw.git.autopull [\#535](https://github.com/CenturyLinkCloud/mdw/issues/535)
- Ability to suppress AssetImportMonitor [\#534](https://github.com/CenturyLinkCloud/mdw/issues/534)
- Support for including boolean "false" values in Jsonable serialization [\#533](https://github.com/CenturyLinkCloud/mdw/issues/533)
- Custom boot jar location for CLI mdw run [\#532](https://github.com/CenturyLinkCloud/mdw/issues/532)
- CLI needs export to PDF [\#529](https://github.com/CenturyLinkCloud/mdw/issues/529)
- CLI should use project.yaml preferentially if present for mdwVersion [\#526](https://github.com/CenturyLinkCloud/mdw/issues/526)
- Link to related process from request page [\#521](https://github.com/CenturyLinkCloud/mdw/issues/521)
- Externalize Active MQ storage directory in application-context.xml [\#520](https://github.com/CenturyLinkCloud/mdw/issues/520)
- Allow arbitrary options for Embedded DB [\#519](https://github.com/CenturyLinkCloud/mdw/issues/519)
- Implement ProcessDueDateMonitor [\#516](https://github.com/CenturyLinkCloud/mdw/issues/516)
- User info changes do not reflect in a clustered environment [\#513](https://github.com/CenturyLinkCloud/mdw/issues/513)
- Monitoring configuration [\#509](https://github.com/CenturyLinkCloud/mdw/issues/509)
- Explode node\_modules.zip in client asset packages [\#508](https://github.com/CenturyLinkCloud/mdw/issues/508)
- Multiple task indices supported in API and Hub task list page [\#501](https://github.com/CenturyLinkCloud/mdw/issues/501)
- Expose draw.io import capability in CLI [\#485](https://github.com/CenturyLinkCloud/mdw/issues/485)
- Edit pre/post script in MDWHub process editor [\#448](https://github.com/CenturyLinkCloud/mdw/issues/448)
- Automated tests for CLI [\#335](https://github.com/CenturyLinkCloud/mdw/issues/335)

**Closed issues:**

- Subprocess activities in Hub definition view = issues when ' v' included in name [\#544](https://github.com/CenturyLinkCloud/mdw/issues/544)
- Fix CLI "install" test to avoid GitHub releases 403 response [\#543](https://github.com/CenturyLinkCloud/mdw/issues/543)
- Kafka adapter throws java.sql.SQLException: Unable to find CREATE\_DT for ADAPTER: 571752 [\#542](https://github.com/CenturyLinkCloud/mdw/issues/542)
- Filepanel cannot parse/display files containing characters outside of UTF-8 set [\#530](https://github.com/CenturyLinkCloud/mdw/issues/530)
- CLI tests fail for pure spring boot jars [\#527](https://github.com/CenturyLinkCloud/mdw/issues/527)
- Issue binding output child process variables back to parent for Invoke Sub Process activity [\#523](https://github.com/CenturyLinkCloud/mdw/issues/523)
- In failed activity list in Hub, the links for master request and process are backwards [\#522](https://github.com/CenturyLinkCloud/mdw/issues/522)
- HTML Process Exporter fixes [\#512](https://github.com/CenturyLinkCloud/mdw/issues/512)
- Rest Adapters should error/fail when they receive a non-2xx HTTP code [\#511](https://github.com/CenturyLinkCloud/mdw/issues/511)
- Postman autotests can crash when server is running in daemon mode [\#510](https://github.com/CenturyLinkCloud/mdw/issues/510)
- ScheduledJob exception should not interfere with server startup [\#507](https://github.com/CenturyLinkCloud/mdw/issues/507)
- Workflow runtime rendering for specific activity/task instances uses latest process definition [\#504](https://github.com/CenturyLinkCloud/mdw/issues/504)
- Manual task completion in package-level error handlers [\#484](https://github.com/CenturyLinkCloud/mdw/issues/484)

## [6.1.09](https://github.com/CenturyLinkCloud/mdw/tree/6.1.09) (2018-09-14)

[Full Changelog](https://github.com/CenturyLinkCloud/mdw/compare/6.1.08...6.1.09)

**Implemented enhancements:**

- Auto-increment option when conflicts during asset import [\#324](https://github.com/CenturyLinkCloud/mdw/issues/324)
- Create reports using MongoDB [\#264](https://github.com/CenturyLinkCloud/mdw/issues/264)
- WorkflowServices should provide filtering process instances by variable value [\#40](https://github.com/CenturyLinkCloud/mdw/issues/40)
- Support @Activity annotations for .java or .kt assets instead of separate .impl file [\#500](https://github.com/CenturyLinkCloud/mdw/issues/500)
- Redirect to intended path when navigating with JWT in query param [\#499](https://github.com/CenturyLinkCloud/mdw/issues/499)
- Asset\_ref DB table should not contain version 0 assets [\#495](https://github.com/CenturyLinkCloud/mdw/issues/495)
- Kotlin language activity implementors [\#494](https://github.com/CenturyLinkCloud/mdw/issues/494)
- Search parent packages for error handler processes [\#493](https://github.com/CenturyLinkCloud/mdw/issues/493)
- Parameterized Postman test case environment files [\#489](https://github.com/CenturyLinkCloud/mdw/issues/489)
- Allow task name query param in MDWHub to populate search string [\#488](https://github.com/CenturyLinkCloud/mdw/issues/488)
- MDWHub Task search params from query string [\#486](https://github.com/CenturyLinkCloud/mdw/issues/486)
- Additional export options for MDW CLI [\#467](https://github.com/CenturyLinkCloud/mdw/issues/467)

**Closed issues:**

- Create Dashboard reports using MongoDB [\#148](https://github.com/CenturyLinkCloud/mdw/issues/148)
- Create Process Heat Map [\#44](https://github.com/CenturyLinkCloud/mdw/issues/44)
- Auto asset import is not triggered in certain cases in clustered envs [\#502](https://github.com/CenturyLinkCloud/mdw/issues/502)
- Process instances of renamed or removed processes cannot be viewed in Hub [\#498](https://github.com/CenturyLinkCloud/mdw/issues/498)
- Process instances of removed/renamed process end up being created without a package [\#496](https://github.com/CenturyLinkCloud/mdw/issues/496)
- Task instance failing when rule based prioritization strategy is speciified [\#491](https://github.com/CenturyLinkCloud/mdw/issues/491)
- Use all\_tab\_cols in db self-upgrade check for Oracle [\#490](https://github.com/CenturyLinkCloud/mdw/issues/490)
- Error accessing Hub for spring boot app with mdw dependency and not in dev mode [\#487](https://github.com/CenturyLinkCloud/mdw/issues/487)

## [6.1.08](https://github.com/CenturyLinkCloud/mdw/tree/6.1.08) (2018-08-31)

[Full Changelog](https://github.com/CenturyLinkCloud/mdw/compare/6.1.07...6.1.08)

**Implemented enhancements:**

- Allow for use of different Mongo database besides "mdw" [\#478](https://github.com/CenturyLinkCloud/mdw/issues/478)

**Closed issues:**

- Ignore .mdw directory with no package.yaml/json [\#483](https://github.com/CenturyLinkCloud/mdw/issues/483)
- Issue with AssetRefCache when retrieving entire list instead of initial setting of last 2 years [\#482](https://github.com/CenturyLinkCloud/mdw/issues/482)
- Hub always renders instance using current process version when using "source" link from child [\#481](https://github.com/CenturyLinkCloud/mdw/issues/481)
- After asset import, ASSET\_REF table is updated with incorrect asset versions [\#480](https://github.com/CenturyLinkCloud/mdw/issues/480)
- FilePanel no longer works when using MDW node 6.1.07 package [\#479](https://github.com/CenturyLinkCloud/mdw/issues/479)
- Error creating assets in newly-created package in MDWHub [\#461](https://github.com/CenturyLinkCloud/mdw/issues/461)

## [6.1.07](https://github.com/CenturyLinkCloud/mdw/tree/6.1.07) (2018-08-10)

[Full Changelog](https://github.com/CenturyLinkCloud/mdw/compare/6.1.06...6.1.07)

**Implemented enhancements:**

- Handle multipart form requests  [\#432](https://github.com/CenturyLinkCloud/mdw/issues/432)
- Migrate MongoDB support to an asset package [\#470](https://github.com/CenturyLinkCloud/mdw/issues/470)
- Upgrade to Spring 5.0 and Spring Boot 2.0 [\#469](https://github.com/CenturyLinkCloud/mdw/issues/469)
- Refactor how Asset imports take place [\#454](https://github.com/CenturyLinkCloud/mdw/issues/454)
- Avoid triggering cache refresh when committing locally [\#452](https://github.com/CenturyLinkCloud/mdw/issues/452)
- Transparent integration into Spring Boot apps [\#439](https://github.com/CenturyLinkCloud/mdw/issues/439)
- Mesos/Marathon Support [\#411](https://github.com/CenturyLinkCloud/mdw/issues/411)
- CLI init to specified directory [\#382](https://github.com/CenturyLinkCloud/mdw/issues/382)

**Closed issues:**

- Document process variable update in child service process not reflected in parent process [\#477](https://github.com/CenturyLinkCloud/mdw/issues/477)
- CLI Import deletes non-mdw local assets [\#476](https://github.com/CenturyLinkCloud/mdw/issues/476)
- Javadocs are not getting published during the build [\#475](https://github.com/CenturyLinkCloud/mdw/issues/475)
- Problem with editing/displaying boolean type process instance variable values in Hub [\#474](https://github.com/CenturyLinkCloud/mdw/issues/474)
- Websockets for pure Spring Boot mode [\#473](https://github.com/CenturyLinkCloud/mdw/issues/473)
- Spring Boot initial request failure [\#472](https://github.com/CenturyLinkCloud/mdw/issues/472)

## [6.1.06](https://github.com/CenturyLinkCloud/mdw/tree/6.1.06) (2018-08-01)
**Implemented enhancements:**

- Support METAINFO\_DOWNLOAD\_FORMAT concept for POST/PUT REST requests [\#465](https://github.com/CenturyLinkCloud/mdw/issues/465)
- For mdw auth, cache central app users locally [\#435](https://github.com/CenturyLinkCloud/mdw/issues/435)
- Startup validation of framework asset packages [\#378](https://github.com/CenturyLinkCloud/mdw/issues/378)
- DB upgrade scripts for 6.1 [\#366](https://github.com/CenturyLinkCloud/mdw/issues/366)
- Retain filter selections in the MDWHub task list [\#364](https://github.com/CenturyLinkCloud/mdw/issues/364)
- Remove old-style Action and Resource services [\#361](https://github.com/CenturyLinkCloud/mdw/issues/361)
- Codegen for CLI [\#356](https://github.com/CenturyLinkCloud/mdw/issues/356)
- Retire mdw-buildpack [\#353](https://github.com/CenturyLinkCloud/mdw/issues/353)
- Ability to display ASSET\_REFed previous version instance list [\#352](https://github.com/CenturyLinkCloud/mdw/issues/352)
- Support Sendgrid for email notifications [\#345](https://github.com/CenturyLinkCloud/mdw/issues/345)
- Replace package.json in .mdw folder with package.yaml [\#339](https://github.com/CenturyLinkCloud/mdw/issues/339)
- Remove Archive directory creation from Import [\#338](https://github.com/CenturyLinkCloud/mdw/issues/338)
- REST services - support for DELETE that includes a body [\#336](https://github.com/CenturyLinkCloud/mdw/issues/336)
- Change MDW DB connections' AutoCommit default to true [\#330](https://github.com/CenturyLinkCloud/mdw/issues/330)
- More flexibility in access.yaml [\#329](https://github.com/CenturyLinkCloud/mdw/issues/329)
- Better CLI support for yaml config [\#325](https://github.com/CenturyLinkCloud/mdw/issues/325)
- FilePanel download with multiserver config [\#321](https://github.com/CenturyLinkCloud/mdw/issues/321)
- FilePanel grep [\#319](https://github.com/CenturyLinkCloud/mdw/issues/319)
- Handle request timeouts during Git asset import [\#311](https://github.com/CenturyLinkCloud/mdw/issues/311)
- Add memory and top functionality in system tab [\#304](https://github.com/CenturyLinkCloud/mdw/issues/304)
- Add log statements to ASSET\_REF table updating to better identify when it happens [\#297](https://github.com/CenturyLinkCloud/mdw/issues/297)
- Arbitrary attributes for Users and Workgroups [\#289](https://github.com/CenturyLinkCloud/mdw/issues/289)
- Upload release artifacts from travis build [\#288](https://github.com/CenturyLinkCloud/mdw/issues/288)
- Remove auto generated date stamp in javadoc [\#281](https://github.com/CenturyLinkCloud/mdw/issues/281)
- Indexes for arbitrary instance-level owners [\#279](https://github.com/CenturyLinkCloud/mdw/issues/279)
- MDWHub - show server name of activity instance/process instance [\#278](https://github.com/CenturyLinkCloud/mdw/issues/278)
- Create new field for document\_id in  MongoDB  [\#277](https://github.com/CenturyLinkCloud/mdw/issues/277)
- Move mdw-hub off of bower [\#272](https://github.com/CenturyLinkCloud/mdw/issues/272)
- Slack app routing and general MDW App token cloud routing [\#271](https://github.com/CenturyLinkCloud/mdw/issues/271)
- Refine com.centurylink.mdw.slack/SlackActivity [\#270](https://github.com/CenturyLinkCloud/mdw/issues/270)
- Execute CLI Checkpoint on server startup [\#265](https://github.com/CenturyLinkCloud/mdw/issues/265)
- FilePanel replacement in React/JSX [\#258](https://github.com/CenturyLinkCloud/mdw/issues/258)
- YAML format for mdw.properties [\#257](https://github.com/CenturyLinkCloud/mdw/issues/257)
- Better handling of asset versions to prevent inflight issues caused by failure to increment [\#256](https://github.com/CenturyLinkCloud/mdw/issues/256)
- Improve Git status monitoring in MDWHub and during import [\#254](https://github.com/CenturyLinkCloud/mdw/issues/254)
- Active activities should not be retried [\#251](https://github.com/CenturyLinkCloud/mdw/issues/251)
- REST API for Saving/Pushing Assets without Deploying [\#248](https://github.com/CenturyLinkCloud/mdw/issues/248)
- Kafka Listener [\#247](https://github.com/CenturyLinkCloud/mdw/issues/247)
- Kafka Adapter Activity [\#246](https://github.com/CenturyLinkCloud/mdw/issues/246)
- Enhance MySQL and Oracle clean-up scripts [\#244](https://github.com/CenturyLinkCloud/mdw/issues/244)
- Provide a filter option to show all processes regardless of status [\#242](https://github.com/CenturyLinkCloud/mdw/issues/242)
- Task UI Integration Fixes [\#239](https://github.com/CenturyLinkCloud/mdw/issues/239)
- Error message on failed login attempt [\#238](https://github.com/CenturyLinkCloud/mdw/issues/238)
- Execute autotests from MDW CLI [\#237](https://github.com/CenturyLinkCloud/mdw/issues/237)
- DynamicJavaActivity is compiled for only a single version. [\#232](https://github.com/CenturyLinkCloud/mdw/issues/232)
- Rules as a service in Drools package [\#228](https://github.com/CenturyLinkCloud/mdw/issues/228)
- JWT Authentication for services [\#222](https://github.com/CenturyLinkCloud/mdw/issues/222)
- Runtime determination of service/nonservice process nature [\#220](https://github.com/CenturyLinkCloud/mdw/issues/220)
- Allow creaton of data sources using any app-provided driver [\#217](https://github.com/CenturyLinkCloud/mdw/issues/217)
- Discover assets from Maven repositories [\#215](https://github.com/CenturyLinkCloud/mdw/issues/215)
- Search by Solution ID in the Workflow tab typeahead [\#214](https://github.com/CenturyLinkCloud/mdw/issues/214)
- Need to upgrade Linux and OSX JARs for our embedded MariaDB to support issue \#178 [\#211](https://github.com/CenturyLinkCloud/mdw/issues/211)
- Populate asset archive from Git instead of during import [\#207](https://github.com/CenturyLinkCloud/mdw/issues/207)
- Support link type Straight in workflow process canvas [\#200](https://github.com/CenturyLinkCloud/mdw/issues/200)
- Support forwarding headers for ClearTrust auth in PaaS [\#199](https://github.com/CenturyLinkCloud/mdw/issues/199)
- Task UI JSX Discussion page [\#195](https://github.com/CenturyLinkCloud/mdw/issues/195)
- Task UI JSX History page [\#194](https://github.com/CenturyLinkCloud/mdw/issues/194)
- Task UI JSX Subtasks page [\#193](https://github.com/CenturyLinkCloud/mdw/issues/193)
- Task UI JSX Values page [\#192](https://github.com/CenturyLinkCloud/mdw/issues/192)
- Task UI revamp using React JSX assets [\#191](https://github.com/CenturyLinkCloud/mdw/issues/191)
- For REST service adapters, populate standard message if the HTTP response msg is empty [\#190](https://github.com/CenturyLinkCloud/mdw/issues/190)
- Formatting options for request and response content [\#189](https://github.com/CenturyLinkCloud/mdw/issues/189)
- Show process name in tool tip of Charts [\#185](https://github.com/CenturyLinkCloud/mdw/issues/185)
- Millisecond precision for db TIMESTAMP and DATETIME fields [\#178](https://github.com/CenturyLinkCloud/mdw/issues/178)
- Include mdw-cli and mdw-boot jars in main build. [\#175](https://github.com/CenturyLinkCloud/mdw/issues/175)
- Upgrade to version 2.x from 1.4 for commons-dbcp and commons-pool  [\#172](https://github.com/CenturyLinkCloud/mdw/issues/172)
- MySQL cleanup script implementation [\#168](https://github.com/CenturyLinkCloud/mdw/issues/168)
- Add Schedule Task to run random test cases [\#164](https://github.com/CenturyLinkCloud/mdw/issues/164)
- Configurable Retries for HTTP-based adapters based on return code [\#162](https://github.com/CenturyLinkCloud/mdw/issues/162)
- Custom report to show Order mean time [\#159](https://github.com/CenturyLinkCloud/mdw/issues/159)
- Allow creation of custom database sources in asset packages [\#157](https://github.com/CenturyLinkCloud/mdw/issues/157)
- Validate against malformed JSON content in JsonRestService [\#155](https://github.com/CenturyLinkCloud/mdw/issues/155)
- React component assets for task page customization [\#150](https://github.com/CenturyLinkCloud/mdw/issues/150)
- Add GitHub Authentication [\#149](https://github.com/CenturyLinkCloud/mdw/issues/149)
- Client project as Spring Boot application [\#145](https://github.com/CenturyLinkCloud/mdw/issues/145)
- Support Tomcat war installation in MDW CLI [\#144](https://github.com/CenturyLinkCloud/mdw/issues/144)
- Allow REST services to accept both Json and Xml type requests [\#141](https://github.com/CenturyLinkCloud/mdw/issues/141)
- Caching for Swagger annotations scan results [\#131](https://github.com/CenturyLinkCloud/mdw/issues/131)
- Redesign event waits/publishing paradigm [\#129](https://github.com/CenturyLinkCloud/mdw/issues/129)
- Autovalidations based on Swagger API Docs [\#126](https://github.com/CenturyLinkCloud/mdw/issues/126)
- Microservice API automated tests [\#125](https://github.com/CenturyLinkCloud/mdw/issues/125)
- MDW CLI for quickstart setup [\#124](https://github.com/CenturyLinkCloud/mdw/issues/124)
- Kotlin ScriptExecutor implementation [\#123](https://github.com/CenturyLinkCloud/mdw/issues/123)
- Enhance DB transactions to retry in case of deadlock/lock timeouts automatically [\#116](https://github.com/CenturyLinkCloud/mdw/issues/116)
- Upgrade to swagger-ui 3.x and enable Try operations [\#114](https://github.com/CenturyLinkCloud/mdw/issues/114)
- Task features in MDWHub [\#112](https://github.com/CenturyLinkCloud/mdw/issues/112)
- Oracle support on MDW 6 [\#109](https://github.com/CenturyLinkCloud/mdw/issues/109)
- Package-based routing and scalability [\#108](https://github.com/CenturyLinkCloud/mdw/issues/108)
- Microservice REST adapter and ServiceSummary concept [\#106](https://github.com/CenturyLinkCloud/mdw/issues/106)
- Extensibility for Hub-UI tabs [\#104](https://github.com/CenturyLinkCloud/mdw/issues/104)
- Self-serialization for Jsonables [\#103](https://github.com/CenturyLinkCloud/mdw/issues/103)
- View and delete asset archive [\#101](https://github.com/CenturyLinkCloud/mdw/issues/101)
- MDW deployable as a Spring Boot application [\#100](https://github.com/CenturyLinkCloud/mdw/issues/100)
- In Hub-UI, display process instances that use previous process definition versions. [\#99](https://github.com/CenturyLinkCloud/mdw/issues/99)
- Internal/Enterprise package supporting WebSphere MQ messaging [\#98](https://github.com/CenturyLinkCloud/mdw/issues/98)
- Implement a GitHub Integration to trigger autotest execution in AppFog [\#97](https://github.com/CenturyLinkCloud/mdw/issues/97)
- AppFog should deploy mdw-buildpack from the GitHub repository [\#95](https://github.com/CenturyLinkCloud/mdw/issues/95)
- Add sonarqube analysis to our CI builds [\#79](https://github.com/CenturyLinkCloud/mdw/issues/79)
- Retain user filter selections on the Workflow tab [\#78](https://github.com/CenturyLinkCloud/mdw/issues/78)
- Distributable hub-ui Angular module for embedable workflow view [\#72](https://github.com/CenturyLinkCloud/mdw/issues/72)
- OpenID Connect authentication [\#69](https://github.com/CenturyLinkCloud/mdw/issues/69)
- EventHandlers and ActivityImpls should be full-blown workflow assets. [\#66](https://github.com/CenturyLinkCloud/mdw/issues/66)
- Test Case HTTP support for net proxy config [\#51](https://github.com/CenturyLinkCloud/mdw/issues/51)
- Switch mdw-demo to use Gradle instead of Maven [\#48](https://github.com/CenturyLinkCloud/mdw/issues/48)
- Display process and activity markdown documentation in MDWHub [\#46](https://github.com/CenturyLinkCloud/mdw/issues/46)
- Create Pie Chart reports in MDWHub [\#43](https://github.com/CenturyLinkCloud/mdw/issues/43)
- Automated builds for mdw6 [\#41](https://github.com/CenturyLinkCloud/mdw/issues/41)
- Create OAuth Server Instance in AppFog [\#37](https://github.com/CenturyLinkCloud/mdw/issues/37)
- Task Event Wait registration service [\#34](https://github.com/CenturyLinkCloud/mdw/issues/34)
- Tag build in Git from Jenkins [\#29](https://github.com/CenturyLinkCloud/mdw/issues/29)
- Import ad-hoc packages into an environment using Admin UI [\#28](https://github.com/CenturyLinkCloud/mdw/issues/28)
- Display package configuration in Assets view [\#25](https://github.com/CenturyLinkCloud/mdw/issues/25)
- Advanced button for User attributes and Solutions values [\#24](https://github.com/CenturyLinkCloud/mdw/issues/24)
- Add ability to run test cases from mdw-admin GUI [\#20](https://github.com/CenturyLinkCloud/mdw/issues/20)
- Add ability to create a Task Template from mdw-admin [\#19](https://github.com/CenturyLinkCloud/mdw/issues/19)
- Docker image for mdw6 [\#12](https://github.com/CenturyLinkCloud/mdw/issues/12)
- Populate statusCode and statusMessage on listener response documents [\#11](https://github.com/CenturyLinkCloud/mdw/issues/11)
- Save protocol metadata as part of request and response documents [\#10](https://github.com/CenturyLinkCloud/mdw/issues/10)
- Asset editing through MDWHub [\#9](https://github.com/CenturyLinkCloud/mdw/issues/9)
- Publicly available discovery site for asset imports [\#5](https://github.com/CenturyLinkCloud/mdw/issues/5)
- Publish mdw 6.0.x war/jars to Maven Central [\#3](https://github.com/CenturyLinkCloud/mdw/issues/3)
- Camel support in mdw6 [\#2](https://github.com/CenturyLinkCloud/mdw/issues/2)
- Export/Import process commands for CLI [\#453](https://github.com/CenturyLinkCloud/mdw/issues/453)
- CLI snapshots base URL should not be hardcoded [\#444](https://github.com/CenturyLinkCloud/mdw/issues/444)
- Replace MDWWeb/docs path in \*.impl pagelets [\#443](https://github.com/CenturyLinkCloud/mdw/issues/443)
- Friendly API for logging service requests/responses \(with headers\) [\#429](https://github.com/CenturyLinkCloud/mdw/issues/429)
- Render option render=text for .md templates for SMS [\#428](https://github.com/CenturyLinkCloud/mdw/issues/428)
- Allow passed in JWT to reach the service [\#425](https://github.com/CenturyLinkCloud/mdw/issues/425)
- Ability to restrict GET service access  [\#424](https://github.com/CenturyLinkCloud/mdw/issues/424)
- Populate INSTANCE\_TIMING table for request responses [\#420](https://github.com/CenturyLinkCloud/mdw/issues/420)
- WYSIWYG markdown editor in MDWHub [\#418](https://github.com/CenturyLinkCloud/mdw/issues/418)
- Server-side rendering of Markdown assets to HTML [\#415](https://github.com/CenturyLinkCloud/mdw/issues/415)
- Built-in JWT authentication for RestServiceAdapter Activity [\#414](https://github.com/CenturyLinkCloud/mdw/issues/414)
- Retire "master server" concept [\#403](https://github.com/CenturyLinkCloud/mdw/issues/403)
- Real-time swagger generation for config-based process paths [\#401](https://github.com/CenturyLinkCloud/mdw/issues/401)
- REST service path associations for workflow processes [\#400](https://github.com/CenturyLinkCloud/mdw/issues/400)
- Handle non "mdw" context root for Hub and Services [\#399](https://github.com/CenturyLinkCloud/mdw/issues/399)
- Docker image has "Could not load J2V8 library" error [\#393](https://github.com/CenturyLinkCloud/mdw/issues/393)
- Self awareness for MDW instances for things like asset imports and cache refreshes [\#389](https://github.com/CenturyLinkCloud/mdw/issues/389)
- Create and populate INSTANCE\_TIMING table [\#387](https://github.com/CenturyLinkCloud/mdw/issues/387)
- Solidify Kotlin language support [\#379](https://github.com/CenturyLinkCloud/mdw/issues/379)
- In-place incremental db schema upgrades [\#377](https://github.com/CenturyLinkCloud/mdw/issues/377)
- Replace template references to deprecated PaasPropertyManager  [\#376](https://github.com/CenturyLinkCloud/mdw/issues/376)
- Task instances link from template view page [\#375](https://github.com/CenturyLinkCloud/mdw/issues/375)
- Persist request/meta for REST requests without a body [\#370](https://github.com/CenturyLinkCloud/mdw/issues/370)
- Microservice orchestrator activity [\#365](https://github.com/CenturyLinkCloud/mdw/issues/365)
- Flexibility to allow services access only from XHR in javascript served from same host [\#344](https://github.com/CenturyLinkCloud/mdw/issues/344)
- Mechanism for removing scheduled timer tasks from the database [\#318](https://github.com/CenturyLinkCloud/mdw/issues/318)
- Make MDW CLI Import function check for same-version modified assets [\#267](https://github.com/CenturyLinkCloud/mdw/issues/267)
- Process/Instance hierarchy in MDWHub [\#177](https://github.com/CenturyLinkCloud/mdw/issues/177)

**Closed issues:**

- CLI archive should parse package.yaml [\#381](https://github.com/CenturyLinkCloud/mdw/issues/381)
- CLI command to validate asset versions [\#372](https://github.com/CenturyLinkCloud/mdw/issues/372)
- CLI Install does not find latest snapshot version [\#368](https://github.com/CenturyLinkCloud/mdw/issues/368)
- Fix Task Templates nav link [\#363](https://github.com/CenturyLinkCloud/mdw/issues/363)
- Redirect to login page when navigating in MDWHub when session expires [\#355](https://github.com/CenturyLinkCloud/mdw/issues/355)
- Require that Process definition lookups are fully qualified [\#350](https://github.com/CenturyLinkCloud/mdw/issues/350)
- Remove all unauthenticated service access [\#349](https://github.com/CenturyLinkCloud/mdw/issues/349)
- Activity timeout invalid event instance delete query [\#348](https://github.com/CenturyLinkCloud/mdw/issues/348)
- Standard HTTP response codes for unhandled REST service exceptions [\#340](https://github.com/CenturyLinkCloud/mdw/issues/340)
- Central discovery import fails [\#333](https://github.com/CenturyLinkCloud/mdw/issues/333)
- Custom app DB data source doesn't work after a cache refresh [\#328](https://github.com/CenturyLinkCloud/mdw/issues/328)
- MDWHub CLI command console without MDW\_HOME [\#327](https://github.com/CenturyLinkCloud/mdw/issues/327)
- Polling for automated tests page when no websocket [\#317](https://github.com/CenturyLinkCloud/mdw/issues/317)
- Add the functionality to search process instances with like \(and in\)  condition in variable value [\#312](https://github.com/CenturyLinkCloud/mdw/issues/312)
- Use React production build for non-dev environments [\#309](https://github.com/CenturyLinkCloud/mdw/issues/309)
- Fix Sign Out page [\#307](https://github.com/CenturyLinkCloud/mdw/issues/307)
- IBM MQ adapter throws java.lang.IllegalAccessError under specific circumstances [\#306](https://github.com/CenturyLinkCloud/mdw/issues/306)
- Encrypt Slack Channel in travis yaml [\#303](https://github.com/CenturyLinkCloud/mdw/issues/303)
- Special Event with root name \<\_mdw\_task\_sla\> is not handled by FallbackEventHandler [\#296](https://github.com/CenturyLinkCloud/mdw/issues/296)
- Task creation currently ignores the entered SLA \("Task Due In" field\)  [\#294](https://github.com/CenturyLinkCloud/mdw/issues/294)
- Locked Document selected for update is released prior to update being performed  [\#293](https://github.com/CenturyLinkCloud/mdw/issues/293)
- NullPointerException can occur sometimes in active flows when cache is being refreshed [\#291](https://github.com/CenturyLinkCloud/mdw/issues/291)
- Spring boot does not listen for websocket connections [\#287](https://github.com/CenturyLinkCloud/mdw/issues/287)
- Dynamic service registration omits qualifying package path [\#285](https://github.com/CenturyLinkCloud/mdw/issues/285)
- Publish build using Travis CI [\#284](https://github.com/CenturyLinkCloud/mdw/issues/284)
- Process instance retrieval by name should handle missing package version in comments [\#283](https://github.com/CenturyLinkCloud/mdw/issues/283)
- Recent changes in AggregateDataAccessVcs have broken Requests, Tasks and Activities charts [\#282](https://github.com/CenturyLinkCloud/mdw/issues/282)
- ASSET\_REF update issue on non-dev embedded db environment [\#276](https://github.com/CenturyLinkCloud/mdw/issues/276)
- Fix Attachments API and integrate with Slack [\#274](https://github.com/CenturyLinkCloud/mdw/issues/274)
- Fix issue of documents changing owner\_type when using MongoDB [\#263](https://github.com/CenturyLinkCloud/mdw/issues/263)
- Have MDW change the contextClassLoader to be a CloudClassLoader [\#262](https://github.com/CenturyLinkCloud/mdw/issues/262)
- POST\_run test in process-apis.postman REST API test broken [\#259](https://github.com/CenturyLinkCloud/mdw/issues/259)
- InvokeSubProcessActivity should not insist on all subproc output vars being bound [\#255](https://github.com/CenturyLinkCloud/mdw/issues/255)
- Editing a new file in MDW Hub [\#252](https://github.com/CenturyLinkCloud/mdw/issues/252)
- Issue when legacy recurring event is unregistered once the process completes [\#245](https://github.com/CenturyLinkCloud/mdw/issues/245)
- Create Demo [\#243](https://github.com/CenturyLinkCloud/mdw/issues/243)
- Activity implementor source link broken [\#240](https://github.com/CenturyLinkCloud/mdw/issues/240)
- Process Values Autoform Fixes [\#236](https://github.com/CenturyLinkCloud/mdw/issues/236)
- Ugly startup exception when running locally with project created via "mdw init" [\#227](https://github.com/CenturyLinkCloud/mdw/issues/227)
- Cannot set values for unpopulated variables in Runtime UI [\#226](https://github.com/CenturyLinkCloud/mdw/issues/226)
- Handle incoming JWT token when using Layer 7 Gateway authentication  [\#221](https://github.com/CenturyLinkCloud/mdw/issues/221)
- Adapters should save a blank request with associated meta for HTTP GET operations [\#219](https://github.com/CenturyLinkCloud/mdw/issues/219)
- Javadocs are not produced correctly by the MDW build [\#216](https://github.com/CenturyLinkCloud/mdw/issues/216)
- Duplicate master request IDs in Workflow tab typeahead [\#213](https://github.com/CenturyLinkCloud/mdw/issues/213)
- MDW-hub’s variables section for process not renter properly [\#212](https://github.com/CenturyLinkCloud/mdw/issues/212)
- RuntimeUI always displays the latest definition of a process [\#209](https://github.com/CenturyLinkCloud/mdw/issues/209)
- Master task should wait to be completed before all Subtasks are completed [\#206](https://github.com/CenturyLinkCloud/mdw/issues/206)
- Suitable error flagging on unsupported subprocess invocations [\#205](https://github.com/CenturyLinkCloud/mdw/issues/205)
- Fix mdw version in mdw-cli when using SNAPSHOT build [\#202](https://github.com/CenturyLinkCloud/mdw/issues/202)
- Fix display of process instance image on Activity page. [\#201](https://github.com/CenturyLinkCloud/mdw/issues/201)
- Charts losing information when filters are selected [\#186](https://github.com/CenturyLinkCloud/mdw/issues/186)
- Gradle custom task plugin changes prevent publishing mdw-templates zip artifact [\#184](https://github.com/CenturyLinkCloud/mdw/issues/184)
- Removing support for AdapterConnectionPool [\#173](https://github.com/CenturyLinkCloud/mdw/issues/173)
- Defensive version sanity check during asset import [\#167](https://github.com/CenturyLinkCloud/mdw/issues/167)
- Fix Chart Type selection [\#163](https://github.com/CenturyLinkCloud/mdw/issues/163)
- Runtime UI can overlook subprocess instances for superseded definitions [\#161](https://github.com/CenturyLinkCloud/mdw/issues/161)
- Postman autotests fail on Cloud Foundry \(at least AppFog\) [\#154](https://github.com/CenturyLinkCloud/mdw/issues/154)
- ConcurrentModificationException in CloudClassLoader [\#151](https://github.com/CenturyLinkCloud/mdw/issues/151)
- Asset discovery service auto-includes asset subpackages [\#138](https://github.com/CenturyLinkCloud/mdw/issues/138)
- Intra-MDW dependencies incorrectly reflected in published .pom artifacts [\#132](https://github.com/CenturyLinkCloud/mdw/issues/132)
- Cross-VM fix for event processing deadlocks [\#122](https://github.com/CenturyLinkCloud/mdw/issues/122)
- Date filter in MDWHub Processes/Requests pages breaks when using cookieStore [\#119](https://github.com/CenturyLinkCloud/mdw/issues/119)
- Process updates not reflected after cache refresh [\#118](https://github.com/CenturyLinkCloud/mdw/issues/118)
- Deadlock in ServiceSummary event processing [\#115](https://github.com/CenturyLinkCloud/mdw/issues/115)
- Tomcat 8.5 status messages always "OK" for REST services [\#113](https://github.com/CenturyLinkCloud/mdw/issues/113)
- Cannot Discover assets for import in mdw-hub from repo on 143 server [\#91](https://github.com/CenturyLinkCloud/mdw/issues/91)
- Not able to deploy mdw-demo to PCF environment [\#89](https://github.com/CenturyLinkCloud/mdw/issues/89)
- Stopping InProgress Test Cases when server shuts down [\#83](https://github.com/CenturyLinkCloud/mdw/issues/83)
- Add workgroups and roles to a user with previous end-dated rows [\#77](https://github.com/CenturyLinkCloud/mdw/issues/77)
- MDW Hub Process UI view does not show correct color on waiting activity of a completed process [\#74](https://github.com/CenturyLinkCloud/mdw/issues/74)
- HTTP response status codes on REST requests [\#73](https://github.com/CenturyLinkCloud/mdw/issues/73)
- Deleting a jar asset causes runtime problems [\#67](https://github.com/CenturyLinkCloud/mdw/issues/67)
- Swagger page Try It button is non-functional [\#64](https://github.com/CenturyLinkCloud/mdw/issues/64)
- Swagger and Browser issues [\#57](https://github.com/CenturyLinkCloud/mdw/issues/57)
- mdw-admin caching issue [\#27](https://github.com/CenturyLinkCloud/mdw/issues/27)
- Newly-added user sometimes not reflected in user list [\#26](https://github.com/CenturyLinkCloud/mdw/issues/26)
- Add group where existing with null end date [\#23](https://github.com/CenturyLinkCloud/mdw/issues/23)
- Drools tests are failing [\#15](https://github.com/CenturyLinkCloud/mdw/issues/15)
- Runtime UI shows duplicate subproc instances when multiple logical flows are mapped [\#464](https://github.com/CenturyLinkCloud/mdw/issues/464)
- Transitions with non-null, empty string Result values do not act as default transitions  [\#458](https://github.com/CenturyLinkCloud/mdw/issues/458)
- CLI update appears to not correctly handling snapshots [\#442](https://github.com/CenturyLinkCloud/mdw/issues/442)
- JDBC SQL Adapter activity does not execute pre-Script [\#436](https://github.com/CenturyLinkCloud/mdw/issues/436)
- Archived process request mappings cause conflicts [\#433](https://github.com/CenturyLinkCloud/mdw/issues/433)
- Missing package versions file prevents MDW startup [\#430](https://github.com/CenturyLinkCloud/mdw/issues/430)
- Default activity attributes not saved in MDWHub asset editor [\#427](https://github.com/CenturyLinkCloud/mdw/issues/427)
- Workflow tab process instance item in Firefox [\#417](https://github.com/CenturyLinkCloud/mdw/issues/417)
- Cluttered Travis build log output [\#416](https://github.com/CenturyLinkCloud/mdw/issues/416)
- Hub RuntimeUI canvas blurry on retina displays [\#412](https://github.com/CenturyLinkCloud/mdw/issues/412)
- Bulletins not displayed in asset editor [\#410](https://github.com/CenturyLinkCloud/mdw/issues/410)
- Published node asset package doesn't contain .gitignore file  [\#408](https://github.com/CenturyLinkCloud/mdw/issues/408)
- Bulletins not displayed when accessed while in-progress [\#407](https://github.com/CenturyLinkCloud/mdw/issues/407)
- Nuisance logging of terminated websocket connections [\#406](https://github.com/CenturyLinkCloud/mdw/issues/406)
- FilePanelService: Permission Issue. [\#405](https://github.com/CenturyLinkCloud/mdw/issues/405)
- mdw cli update does not work  [\#394](https://github.com/CenturyLinkCloud/mdw/issues/394)
- MDWHub Task template edit always thinks the version is changing [\#391](https://github.com/CenturyLinkCloud/mdw/issues/391)
- Code in REST status response payload should match HTTP status code [\#390](https://github.com/CenturyLinkCloud/mdw/issues/390)
- Runtime libs should not be included in CLI zip [\#388](https://github.com/CenturyLinkCloud/mdw/issues/388)
- CLI should install boot jar to project root [\#367](https://github.com/CenturyLinkCloud/mdw/issues/367)
- CLI init does not completely honor --snapshots [\#362](https://github.com/CenturyLinkCloud/mdw/issues/362)



\* *This Change Log was automatically generated by [github_changelog_generator](https://github.com/skywinder/Github-Changelog-Generator)*
