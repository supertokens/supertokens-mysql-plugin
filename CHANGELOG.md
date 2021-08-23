# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/), and this project adheres
to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [1.9.0] - 2021-06-20

### Added

- Changes for pagination and count queries: https://github.com/supertokens/supertokens-core/issues/259
- Add GetThirdPartyUsersByEmail query: https://github.com/supertokens/supertokens-core/issues/277
- Update user's email via transaction for emailpassword: https://github.com/supertokens/supertokens-core/issues/275
- Added emailverification functions: https://github.com/supertokens/supertokens-core/issues/270

### Fixes

- Fixes issue: https://github.com/supertokens/supertokens-core/issues/258
- Fixes detecting of thirdparty sign up duplicate error.

## [1.8.0] - 2021-04-20

### Added

- Added ability to set table name prefix (https://github.com/supertokens/supertokens-core/issues/220)
- Added connection URI support (https://github.com/supertokens/supertokens-core/issues/221)

## [1.7.0] - 2021-02-16

### Changed

- Extracted email verification as its own recipe
- ThirdParty queries

## [1.6.0] - 2021-01-14

### Changed

- Used rowmapper ineterface
- Adds email verificaiton queries and tables
- Adds user pagination queries

## [1.5.0] - 2020-11-06

### Added

- Support for emailpassword recipe
- Refactoring of queries to put them per recipe
- Changes base interface as per plugin interface 2.4

## [1.3.0] - 2020-05-21

### Added

- Adds check to know if in memory db should be used.

## [1.2.2] - 2020-05-14

### Changed

- Updated MariaDB driver to support other authentication types