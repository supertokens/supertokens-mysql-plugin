# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/), and this project adheres
to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Changes

- Support for multitenancy
  - New tables `apps` and `tenants` have been added.
  - Schema of tables have been changed, adding `app_id` and `tenant_id` columns in tables and constraints & indexes have been modified to include this columns.
  - New user tables have been added to map users to apps and tenants.
  - New tables for multitenancy have been added.

### Migration

Ensure the core is already migrated to version 2.21 and then,
Run the following:

```sql
TODO
```

## [3.0.0] - 2023-04-05

- Adds `use_static_key` `BOOLEAN` column into `session_info`
- Adds support for plugin inteface version 2.23

### Migration

- If using `access_token_signing_key_dynamic` false in the core:
  - ```sql
    ALTER TABLE session_info ADD COLUMN use_static_key BOOLEAN NOT NULL DEFAULT true;
    ALTER TABLE session_info ALTER COLUMN use_static_key DROP DEFAULT;
    ```
  - ```sql
    INSERT INTO jwt_signing_keys(key_id, key_string, algorithm, created_at)
      select CONCAT('s-', created_at_time) as key_id, value as key_string, 'RS256' as algorithm, created_at_time as created_at
      from session_access_token_signing_keys;
    ```
- If using `access_token_signing_key_dynamic` true (or not set) in the core:
  - ```sql
    ALTER TABLE session_info ADD COLUMN use_static_key BOOLEAN NOT NULL DEFAULT false;
    ALTER TABLE session_info ALTER COLUMN use_static_key DROP DEFAULT;
    ```

## [2.4.0] - 2023-03-30

- Support for Dashboard Search

## [2.3.0] - 2023-03-27
- Support for TOTP recipe
- Support for active users

### Database changes

- Add new tables for TOTP recipe:
  - `totp_users` that stores the users that have enabled TOTP
  - `totp_user_devices` that stores devices (each device has its own secret) for each user
  - `totp_used_codes` that stores used codes for each user. This is to implement rate limiting and prevent replay attacks.
- Add `user_last_active` table to store the last active time of a user.

## [2.2.0] - 2023-02-21

- Support for Dashboard Recipe

## [2.1.0] - 2022-11-07

- Updates dependencies as per: https://github.com/supertokens/supertokens-core/issues/525

## [2.0.0] - 2022-09-19

- Updates the `third_party_user_id` column in the `thirdparty_users` table from `VARCHAR(128)` to `VARCHAR(256)` to
  resolve https://github.com/supertokens/supertokens-core/issues/306

- Adds support for user migration
    - Updates the `password_hash` column in the `emailpassword_users` table from `VARCHAR(128)` to `VARCHAR(256)` to
      support more types of password hashes.

- For legacy users who are self hosting the SuperTokens core run the following command to update your database with the
  changes:
  `ALTER TABLE thirdparty_users MODIFY third_party_user_id VARCHAR(256); ALTER TABLE emailpassword_users MODIFY password_hash VARCHAR(256);`

## [1.19.0] - 2022-08-18

- Adds log level feature and compatibility with plugin interface 2.18

## [1.18.0] - 2022-08-10

- Adds compatibility with plugin interface 2.17

## [1.17.0] - 2022-07-07

- Support for UserIdMapping

## [1.16.0] - 2022-06-07

- Compatibility with plugin interface 2.15 - returns only non expired session handles for a user

## [1.15.0] - 2022-05-05

- Support for User Roles

## [1.14.0]

### Added

- Support for usermetadata
- Fixes issue https://github.com/supertokens/supertokens-mysql-plugin/issues/31

## [1.13.0] - 2022-02-23

### Changed

- Using lower transaction isolation level while creating passwordless device with code
- Fixed ResultSet instances to avoid Memory Leaks

## [1.12.1] - 2022-02-16

- Fixed https://github.com/supertokens/supertokens-core/issues/373: Catching `StorageTransactionLogicException` in
  transaction helper function for retries
- add workflow to verify if pr title follows conventional commits

## [1.12.0] - 2022-01-14

### Added

- passwordless support

## [1.11.0] - 2021-12-19

### Added

- Delete user functionality

## [1.10.0] - 2021-09-10

### Changed

- Updated to match 2.9 plugin interface to support multiple access token signing
  keys: https://github.com/supertokens/supertokens-core/issues/305
- Added functions and other changes for the JWT recipe

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
