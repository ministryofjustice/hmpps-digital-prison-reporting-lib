Below you can find the changes included in each release.

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
