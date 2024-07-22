Below you can find the changes included in each release.

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
