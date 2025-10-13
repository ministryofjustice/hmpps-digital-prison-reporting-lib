Below you can find the changes included in each release.
# 9.4.10
- Added endpoint to get a single report summary by definitionId

# 9.4.9
- Fixed issue in which when Athena would return "CANCELED" with single "L" the query would not be cancelled.

# 9.4.8
- Fixed the issue in getProductDefinition with assigning always the orphanage path regardless of which path was being passed.

# 9.4.7
- Get single product definition uses DynamoDB with partition and sort keys instead of iterating over the entire list of definitions.

# 9.4.6
- Attempt to ensure getProductDefinitions retrieves from all paths necessary in cache.

# 9.4.5
- The endpoint to retrieve dashboard results now has no default if pageSize query is not provided and it returns all the results in one response.

# 9.4.4
- Added FlywayConfig to AutoConfiguration.imports.

# 9.4.3 
- Fixed the Hikari config of the missingReportDataSource. 

# 9.4.2
- Removed DataSourceConfiguration as the creation of a main DataSource already takes place by defining the default datasource properties.
- Fixed selection of the main DataSource in RepositoryHelper.
- Removed the missingReportDataSourceProperties bean creation as this conflicts with the default DataSourceProperties when Spring tries to create the main DataSource. Now the DataSourceProperties are created from the configuration properties using Binder.

# 9.4.1
- Use the default Spring DataSource by name in the RepositoryHelper when there is no specific one passed as a parameter. 
This fixes an error where Spring fails to find the right DataSource bean.

# 9.4.0
- Added support for missing report submissions

# 9.3.1 - 9.3.3
- Fixed Dynamo DB issue which was causing an error when trying to read from multiple definition categories (paths).

# 9.3.0
- Added optional loadType to variant summary and dashboard summary to define loadType in the data product definition.

# 9.2.1
- Reverted the removal of the Redshift async journey support as this is still being used by platform clients and the alternative
  to run Athena federated queries to Redshift has not been implemented yet to migrate.

# 9.2.0
- Include missing reports in list of reports returned from /definitions endpoint

# 9.1.0
- Includes changes from `8.5.0` and `8.4.3` as these two versions were mistakenly published with more recent changes than `9.0.0` but with lower version numbers.

# 8.4.3
- Changed the sort direction field name in the DPD to be all lowercase for consistency with existing DPD field naming conventions.

# 8.5.0
- Adds support for sortDirection in DPDs

# 9.0.0
- All async queries run via Athena. 
- Removed the Redshift data API status and cancel endpoints as they are not used.
- Added `connection` and `dialect` fields to `Datasource`.
- More information about their usage can be found in the [definitions schema](https://github.com/ministryofjustice/hmpps-digital-prison-reporting-data-product-definitions-schema). 

# 8.4.2
- Adds support for format_number formula

# 8.4.1
- Reports support multiphase queries, including parameters (prompts).
- Unified status endpoint behaviour for both reports and dashboards to check for an existing table for completed queries
before returning the "FINISHED" status.

# 8.4.0
- Changed table name from `execution_manager` to `multiphase_query_state` and column name of this table from `datasource` to `datasource_name`.

# 8.3.5
- Fixed issue with multiphase queries throwing an error when the list of queries contained only a
single query.

# 8.3.4
Added `row-section`and `row-section-child` as supported report template types.

# 8.3.1-alpha.0, 8.3.2 and 8.3.3
Added sequenceNumber to the multiphase query Redhisft table to address the race condition of an earlier state change overwriting a later one.
Added last_update to allow for cleaning up the table.

# 8.2.0 - 8.3.1
Added Alerts as referenceType 'alert' for the alert parameters.

# 8.1.0
Added `parent-child-section` report specification template.

# 8.0.11
Added debug logs to log the result query for the async journey.

# 8.0.3 - 8.0.10
Added support for multiphase queries for dashboards. This is an experimental feature at this point.

# 8.0.2
Renamed dashboard fields which were defining collections from plural to singular to comply with existing naming conventions in DPDs. 

# 8.0.1
Fixed an issue in DprSystemAuthAwareAuthenticationToken due to which an error was thrown when the username was null. 

# 8.0.0
Provide a system credential mechanism to authenticate users interacting with the DPR library. Endpoints can new be secured with a
system token and caseloads and roles are checked my callback to the manage user api service rather than token inspection.

# 7.13.0
Migrate to call manage-users-api instead of the nomis-user-roles-api to retrieve caseloads.

# 7.10.8
New dashboard execution cancellation endpoint to support running dashboards for legacy nomis/bodmis reports.

# 7.10.7
New dashboard status endpoint to support running dashboards for legacy nomis/bodmis reports. 

# 7.10.6
Fixed issue with more than one multiselect filters in which the same filter value was applied to different columns.   

# 7.10.5
Fixed issue with DprResourceServerConfiguration.
Renamed resourceServerCustomizer Bean to dprResourceServerCustomizer and added "paths" property to the securityMatcher. 

# 7.10.4
Secure only endpoints in this library with the roles provided. Removed the default role.

# 7.10.3
Update dashboard definition.

# 7.10.2
Support in dashboards for 'caseloads` and 'multiselect' filter types.

# 7.10.0 - 7.10.1
Integrate the library with the HMPPS Kotlin library and make use of the `AuthAwareAuthenticationToken` and Converter interface.

# 7.9.1
Support for filters of type 'caseloads' and 'multiselect'. 
These are passed as one query parameter per filter with a comma separated list of values:
`filters.someMultiselectFilter=a,b,c`

# 7.9.0
For scheduled datasets, use the generated dataset if it is available. Currently supports datamart datasources.

# 7.8.2
Changed the dashboard definition. The main changes are:
Updated metric definition to section definition which contains a list of DashboardVisualisation definitions.

# 7.8.1
This fixes a bug with the caseload list generation. Was: `MDI,BXI,LEI`. It now is: `'MDI','BXI','LEI'`.

# 7.7.0 - 7.8.0
Support policy checks a list of available caseloads a user is allowed to access. For example:
```json
{
  "policy": [
    {
      "id": "caseloads",
      "type": "row-level",
      "action": [
        "location IN (${caseloads})"
      ],
      "rule": [
        {
          "effect": "permit",
          "condition": [
            {
              "exists": [
                "${caseloads}"
              ]
            }
          ]
        }
      ]
    }
  ]
}
```

# 7.6.0
Dashboard definition response converts dataset parameters to filters.

# 7.5.1
Dashboards support also Athena queries based on the datasource defined in the DPD.

# 7.5.0
Add Parent-Child template: https://github.com/ministryofjustice/hmpps-digital-prison-reporting-data-product-definitions-schema?tab=readme-ov-file#parent-child-template

# 7.4.0
The dashboard result endpoint now returns a ResponseEntity<List<List<Map<String, Any?>>>> instead a ResponseEntity<List<Map<String, Any?>>> which was previously returned.

# 7.3.21
The status endpoints check if a table is missing only if the status is 'FINISHED' and there is a table ID provided.

# 7.3.20
The status endpoints accept an optional tableId to check for table existence before returning the status.

# 7.3.19
Changes to return a 404 instead of 500 when the results endpoints are called and the relevant Redshift table has been removed.

# 7.3.18
Changed numeric quick filter values to whole words. 

# 7.3.17
Fix date formatting issue.
Added defaultGranularity and defaultQuickFilterValue to filters.

# 7.3.16
More information on formula failure.

# 7.3.15
These changes remove the stack trace from the userMessage of the 500 responses and replace it with a generic error message.

# 7.3.14
The creation of DataApiAsyncController is now conditional on dpr.lib.aws.sts.enabled being true.
The NPE try/catch blocks have been removed from the Controller.

# 7.3.13
Change specialType to referenceType for the establishment and wing parameters.
Included RefreshCacheSchedulingService in the AutoConfiguration.imports.

# 7.3.12
Return only the establishment description as the FilterOption display value. 

# 7.3.11
Fix for DDB paging.

# 7.3.10
Fix for STS config.

# 7.3.9
Fix issue with retrieving list of establishments and wings when there is more than one page of results in Athena.

# 7.3.8
Fix issue with calculated properties initialising too soon.

# 7.3.7
Fixed issue with Stopwatch.duration call throwing an exception.

# 7.3.6
Add caching to DDB DPD repo.

# 7.3.3 - 7.3.5
Fix for DynamoDB config.

# 7.3.2
Fix for unnecessary DynamoDB config and dependency issue. 

# 7.3.1
Returning the list of wings as static options. Also added the establishment code suffixed 
with a dash before the description in the list of establishments returned as static options.

# 7.3.0
Added support for DynamoDB storage of DPDs.

## 7.2.9
Added user authorisation check against report policy for sync report definition endpoints. 
Refactored policy engine, so that authorisation check only executes access level PolicyType.

## 7.2.8
Fixed issue with ProductDefinitionTokenPolicyChecker not visible for automatic DI in hosts of the library.

## 7.2.7
Fixed issue with Stopwatch.duration call throwing an exception.

## 7.2.6
Added user authorisation check against report policy for async report definition endpoints.

## 7.2.5
Fixed issue with application startup due to AthenaApiRepository bean ambiguity on autowiring.

## 7.2.4
Caching the list of establishments and wings and returning the establishments as static options.

## 7.2.3
Added authorised flag to Report Summary Definition to determine if a user has access to a report definition.

## 7.2.2
Added sortColumn and sortAsc query parameter to the async results endpoint to support the interactive journey.

## 7.2.1
Added report metadata with support for the interactive hint.

## 7.2.0
Added interactive count endpoint.

## 7.1.1
Fix issue with dashboard query execution.

## 7.1.0
Added filters to getStatementResults endpoint and dashboard definition.

## 7.0.5
Added endpoint to cancel running dashboard queries.

## 7.0.4
Added get dashboard results endpoint.

## 7.0.3
Added statement execution status endpoint.

## 7.0.2
Added async dashboard execution endpoint.

## 7.0.1
Removed unused property

## 7.0.0 
Removed the metrics definition and metrics data endpoints.
Updated the DPD with regard to dashboards and metrics.
Updated the dashboard definition response.

## 6.3.2
Add Crosstab supporting properties

## 6.3.1
Add ContextAuthenticationHelper implementation to autoconfig.

## 6.3.0
Added JWT authentication to Definitions API call.

## 6.2.0
Added new metrics data API.

## 6.1.2
Amended the dashboard and metric definitions by removing visualisationType from the dashboard. Added the optional boolean 'group' and list 'chart' fields to metrics.

## 6.1.1
Removed the /definitions/dashboards endpoint and added the dashboard definitions to the ReportDefinitionSummary.

## 6.1.0
Added three new APIs to retrieve Dashboard and metric definitions.

## 6.0.4
Support for section header summaries.

## 6.0.3
Refactor ConfiguredApiService and split into SyncDataApiService and AsyncDataApiService.

## 6.0.2
Support for Sectioned Summary reports.

## 6.0.1
Added some logging when there are no matching schema fields for the projected columns of the query.

## 6.0.0
Update to: 
- Java 21 (from 19)
- HMPPS Kotlin plugin version 6 (from 5)
- Gradle 8.10 (from 8.1)

## 5.1.5
Amended swagger documentation regarding the use of filters deriving from prompts in the async API.

## 5.1.4
Wrapped date prompts in toDate function while constructing the prompt_ CTE and defaulted the format to yyyy-mm-dd.

## 5.1.3
Defaulted mandatory to false for the report fields generated from DPD parameters (prompts).

## 5.1.2
Defaulted visible to false for the report fields generated from DPD parameters (prompts).

## 5.1.1
Fixed wrong Athena workgroup property value.

## 5.1.0
Athena queries use a workgroup.

## 5.0.3
Added comment with DPD and report details to the Athena query.

## 5.0.2
Fixed issue with format_date formula when the date columns had names other than 'date'. 

## 5.0.1
Renamed prefilter_ CTE to report_ CTE.

## 5.0.0
Added prefilter_ CTE to Redshift synchronous and asynchronous queries.

## 4.12.2
Create summary table synchronously via normal datasource.

## 4.12.1
Add summary table creation logging and fix issue with quotes in query.

## 4.12.0
Addition of a prefilter_ CTE to the Athena queries.

## 4.11.3
Fix summary table check exception handling.

## 4.11.2
Lazy loading of summary data to fix Athena restriction on statements.

## 4.11.1
Changed prompts_ to prompt_ CTE.

## 4.11.0
Prepended context_ CTE to the Athena queries. 

## 4.10.1
Fixed issue with reports that do not have summaries failing. 

## 4.10.0
If the main dataset query has a dataset_ CTE embedded, then the query is used as is, without recreating the dataset_ CTE in the Athena queries.

## 4.9.1
Added debug logs for the async queries.

## 4.9.0
Create the prompts_ CTE from the filters which originated from the DPD parameters and embed it into the Athena query.

## 4.8.0
Support for Report Summary/Aggregate templates.

## 4.7.0
Filters converted from parameters are validated based on whether they have matching parameter names.

## 4.6.0
Support parameters in the DPD dataset and return them as filters in the report definition response.

## 4.5.3
Fix issue with dynamic filter options based on validated filter values. 

## 4.5.2
Fix issue with empty section properties.

## 4.5.1
Added `section` property for `list-section` report templates

## 4.5.0

- Added Text filter type.
- Added `mandatory` property for filter validation.
- Added `pattern` property for filter validation using a regex.
- Added an endpoint to stop asynchronous statements which are still running.

## 4.4.0
Added ability to have filters which reference different datasets other than the main report dataset.

## 4.3.1
Allow dates (as well as datetimes) to be used as a source for format_date.

## 4.3.0
Change the way we resolve Filters and Policy CTEs from WHERE TRUE and WHERE FALSE clauses to WHERE 1=1 and 0=1 respectively  
in the SQL queries in order to provide support also for Oracle.

## 4.2.3
Case-insensitive comparison of Datasource name to determine whether to use the Athena or Redshift API.

## 4.2.2
Fixes the issue in which the Redshift async query was failing when the parameters' values were passed using the ExecuteStatementRequest.Builder.parameters method of the Redshift Data Api.

## 4.2.1
Fixes the issue in which when a request was made with filters then all subsequent requests without filters would fail.

## 4.2.0
New AthenaAPIRepository which queries Athena.\
Datasource supports two new optional fields: database and catalog.\
Added functionality to call the Athena API to start the query execution and retrieve the execution status for nomis and bodmis reports based on the datasource name.
Existing datamart reports will run against the Redshift Data API.
Filters are not supported yet for nomis and bodmis reports as part of this release.\
The `/report/statements/{statementId}/status` endpoint has changed to
`/reports/{reportId}/{reportVariantId}/statements/{statementId}/status`

## 4.1.0
Added count endpoint for the created external tables.

## 4.0.0
The asynchronous Redshift endpoint executes the DPD query and the results are stored into an external table.
The endpoint to retrieve the results queries that table and allows for pagination.
The path of this endpoint has also changed to `/reports/{reportId}/{reportVariantId}/tables/{tableId}/result`

## 3.7.11
Added nextToken query parameter to the getQueryExecutionResult endpoint and 
changed the response of the endpoint to contain the nextToken if it exists to support
pagination.
Added resultSize to the StatementExecutionStatus response.

## 3.7.10
Bug fix of the error thrown when the filters in the request contained dots. 

## 3.7.9
Introduction of the API to get statement result.

## 3.7.8
Bug fix of the asynchronous query execution which was throwing an error when no filters were provided.

## 3.7.7
Report execution status API to get the execution status of queries which were ran asynchronously.  

## 3.7.6
Added dpr.lib prefix to the redshiftdataapi properties. 

## 3.7.5
Introduction of the asynchronous API to execute SQL statements to generate reports and return the execution ID. This is not stable yet.

## 3.7.4
Fixed insecure dependency versions.

## 3.7.2
Fixed the issue in which filtering by Boolean fields would throw an error.

## 3.7.1
Fixed the issue in which null dates would throw an error when the `format_date` function was applied to them. 

## 3.7.0
The JSON schema has been reconciled to the library functionality:

- Changed report-specification.template from a string to an enum with currently just the possible value of `list`.
- Changed report-field.wordwrap enum values from `[ "on", "off", "none" ]`  to `[ "none", "normal", "break-words" ]` .
  - `none` prevents wrapping.
  - `normal`  is normal wrapping behaviour - adding line breaks at the ends of words where necessary.
  - `break-words` allows the text to wrap, breaking words if it has to.
- Removed report-field.type enum values: `null`, `bytes`.
- Added support for the remaining field types. 

## v3.6.0
The library is autoconfigured and there is no longer need of a `@ComponentScan` annotation in the hosting application.

## v3.5.2
Add a 'calculated' property to the controller definition, to show whether a field value is calculated using a formula.

## v3.5.1
Make the report variant display field optional as there is already a fallback to the dataset display field. 

## v3.5.0 
Support for `format_date` formula. Please refer to the [integrating-with-library](https://github.com/ministryofjustice/hmpps-digital-prison-reporting-lib/blob/main/integrating-with-library.md) README file for more details.

## v3.4.0
Added changelog.
