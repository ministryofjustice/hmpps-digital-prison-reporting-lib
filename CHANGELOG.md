Below you can find the changes included in each release.

## v3.4.0
Added changelog.

## v3.5.0 
Support for format_date formula. Please refer to the [integrating-with-library](https://github.com/ministryofjustice/hmpps-digital-prison-reporting-lib/blob/main/integrating-with-library.md) README file for more details.

## v3.5.1
Make the report variant display field optional as there is already a fallback to the dataset display field. 

## v3.5.2
Add a 'calculated' property to the controller definition, to show whether a field value is calculated using a formula.

## v3.6.0
The library is autoconfigured and there is no longer need of a @ComponentScan annotation in the hosting application.

## 3.7.0
The JSON schema has been reconciled to the library functionality:

- Changed report-specification.template from a string to an enum with currently just the possible value of `list`.
- Changed report-field.wordwrap enum values from `[ "on", "off", "none" ]`  to `[ "none", "normal", "break-words" ]` .
  - `none` prevents wrapping.
  - `normal`  is normal wrapping behaviour - adding line breaks at the ends of words where necessary.
  - `break-words` allows the text to wrap, breaking words if it has to.
- Removed report-field.type enum values: `null`, `bytes`.
- Added support for the remaining field types. 

## 3.7.1
Fixed the issue in which null dates would throw an error when the format_date function was applied to them. 