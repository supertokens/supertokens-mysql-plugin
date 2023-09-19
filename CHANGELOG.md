# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/), and this project adheres
to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [5.0.0] - 2023-09-19

### Changes

- Support for Account Linking
  - Adds columns `primary_or_recipe_user_id`, `is_linked_or_is_a_primary_user` and `primary_or_recipe_user_time_joined` to `all_auth_recipe_users` table
  - Adds columns `primary_or_recipe_user_id` and `is_linked_or_is_a_primary_user` to `app_id_to_user_id` table
  - Removes index `all_auth_recipe_users_pagination_index` and addes `all_auth_recipe_users_pagination_index1`, 
    `all_auth_recipe_users_pagination_index2`, `all_auth_recipe_users_pagination_index3` and 
    `all_auth_recipe_users_pagination_index4` indexes instead on `all_auth_recipe_users` table
  - Adds `all_auth_recipe_users_recipe_id_index` on `all_auth_recipe_users` table
  - Adds `all_auth_recipe_users_primary_user_id_index` on `all_auth_recipe_users` table
  - Adds `email` column to `emailpassword_pswd_reset_tokens` table
  - Changes `user_id` foreign key constraint on `emailpassword_pswd_reset_tokens` to `app_id_to_user_id` table

### Migration

1. Ensure that the core is already upgraded to the version 6.0.13 (CDI version 3.0)
2. Stop the core instance(s)
3. Run the migration script
   ```sql
    ALTER TABLE all_auth_recipe_users
      ADD primary_or_recipe_user_id CHAR(36) NOT NULL DEFAULT ('0');

    ALTER TABLE all_auth_recipe_users
      ADD is_linked_or_is_a_primary_user BOOLEAN NOT NULL DEFAULT FALSE;

    ALTER TABLE all_auth_recipe_users
      ADD primary_or_recipe_user_time_joined BIGINT UNSIGNED NOT NULL DEFAULT 0;

    UPDATE all_auth_recipe_users
      SET primary_or_recipe_user_id = user_id
      WHERE primary_or_recipe_user_id = '0';

    UPDATE all_auth_recipe_users
      SET primary_or_recipe_user_time_joined = time_joined
      WHERE primary_or_recipe_user_time_joined = 0;

    ALTER TABLE all_auth_recipe_users
      ADD FOREIGN KEY (app_id, primary_or_recipe_user_id)
      REFERENCES app_id_to_user_id (app_id, user_id) ON DELETE CASCADE;

    ALTER TABLE all_auth_recipe_users
      ALTER primary_or_recipe_user_id DROP DEFAULT;

    ALTER TABLE app_id_to_user_id
      ADD primary_or_recipe_user_id CHAR(36) NOT NULL DEFAULT ('0');

    ALTER TABLE app_id_to_user_id
      ADD is_linked_or_is_a_primary_user BOOLEAN NOT NULL DEFAULT FALSE;

    UPDATE app_id_to_user_id
      SET primary_or_recipe_user_id = user_id
      WHERE primary_or_recipe_user_id = '0';

    ALTER TABLE app_id_to_user_id
      ADD FOREIGN KEY (app_id, primary_or_recipe_user_id)
      REFERENCES app_id_to_user_id (app_id, user_id) ON DELETE CASCADE;

    ALTER TABLE app_id_to_user_id
      ALTER primary_or_recipe_user_id DROP DEFAULT;

    DROP INDEX all_auth_recipe_users_pagination_index ON all_auth_recipe_users;

    CREATE INDEX all_auth_recipe_users_pagination_index1 ON all_auth_recipe_users (
      app_id, tenant_id, primary_or_recipe_user_time_joined DESC, primary_or_recipe_user_id DESC);

    CREATE INDEX all_auth_recipe_users_pagination_index2 ON all_auth_recipe_users (
      app_id, tenant_id, primary_or_recipe_user_time_joined ASC, primary_or_recipe_user_id DESC);

    CREATE INDEX all_auth_recipe_users_pagination_index3 ON all_auth_recipe_users (
      recipe_id, app_id, tenant_id, primary_or_recipe_user_time_joined DESC, primary_or_recipe_user_id DESC);

    CREATE INDEX all_auth_recipe_users_pagination_index4 ON all_auth_recipe_users (
      recipe_id, app_id, tenant_id, primary_or_recipe_user_time_joined ASC, primary_or_recipe_user_id DESC);

    CREATE INDEX all_auth_recipe_users_primary_user_id_index ON all_auth_recipe_users (primary_or_recipe_user_id, app_id);

    CREATE INDEX all_auth_recipe_users_recipe_id_index ON all_auth_recipe_users (app_id, recipe_id, tenant_id);

    ALTER TABLE emailpassword_pswd_reset_tokens 
      DROP FOREIGN KEY emailpassword_pswd_reset_tokens_ibfk_1;

    ALTER TABLE emailpassword_pswd_reset_tokens
      ADD FOREIGN KEY (app_id, user_id) REFERENCES app_id_to_user_id (app_id, user_id) ON DELETE CASCADE;

    ALTER TABLE emailpassword_pswd_reset_tokens ADD email VARCHAR(256);
   ```
4. Run the new instance(s) of the core (version 7.0.0)

## [4.0.2]

- Fixes null pointer issue when user belongs to no tenant.

## [4.0.1] - 2023-07-11

- Fixes duplicate users in users search queries when user is associated to multiple tenants


## [4.0.0] - 2023-06-02

### Changes

- Support for multitenancy
  - New tables `apps` and `tenants` have been added.
  - Schema of tables have been changed, adding `app_id` and `tenant_id` columns in tables and constraints & indexes have been modified to include this columns.
  - New user tables have been added to map users to apps and tenants.
  - New tables for multitenancy have been added.
- Increased transaction retry count to 50 from 20.

### Migration

1. Ensure that the core is already upgraded to version 5.0.0 (CDI version 2.21)
2. Stop the core instance(s)
3. Run the migration script

    ```sql
    -- helper stored procedures

    CREATE PROCEDURE st_drop_all_fkeys()
    BEGIN
      DECLARE done INT DEFAULT FALSE;
      DECLARE dropCommand VARCHAR(255);
      DECLARE dropCur CURSOR for 
              SELECT concat('ALTER TABLE ', table_schema,'.',table_name,' DROP FOREIGN KEY ', constraint_name, ';') 
              FROM information_schema.table_constraints
              WHERE constraint_type='FOREIGN KEY' 
                  AND table_schema = DATABASE();

      DECLARE CONTINUE handler for NOT found SET done = true;
        OPEN dropCur;

        read_loop: LOOP
            FETCH dropCur INTO dropCommand;
            IF done THEN
                leave read_loop;
            END IF;

            SET @sdropCommand = dropCommand;

            PREPARE dropClientUpdateKeyStmt FROM @sdropCommand;

            EXECUTE dropClientUpdateKeyStmt;

            DEALLOCATE prepare dropClientUpdateKeyStmt;
        END LOOP;

        CLOSE dropCur;
    END

    ---

    CREATE PROCEDURE st_drop_all_pkeys()
    BEGIN
      DECLARE done INT DEFAULT FALSE;
      DECLARE dropCommand VARCHAR(255);
      DECLARE dropCur CURSOR for 
              SELECT concat('ALTER TABLE ', table_schema,'.',table_name,' DROP PRIMARY KEY ', ';') 
              FROM information_schema.table_constraints
              WHERE constraint_type='PRIMARY KEY' 
                  AND table_schema = DATABASE();

      DECLARE CONTINUE handler for NOT found SET done = true;
        OPEN dropCur;

        read_loop: LOOP
            FETCH dropCur INTO dropCommand;
            IF done THEN
                leave read_loop;
            END IF;

            SET @sdropCommand = dropCommand;

            PREPARE dropClientUpdateKeyStmt FROM @sdropCommand;

            EXECUTE dropClientUpdateKeyStmt;

            DEALLOCATE prepare dropClientUpdateKeyStmt;
        END LOOP;

        CLOSE dropCur;
    END

    ---

    CREATE PROCEDURE st_drop_all_keys()
    BEGIN
      DECLARE done INT DEFAULT FALSE;
      DECLARE dropCommand VARCHAR(255);
      DECLARE dropCur CURSOR for 
              SELECT concat('ALTER TABLE ', table_schema,'.',table_name,' DROP INDEX ', constraint_name, ';') 
              FROM information_schema.table_constraints
              WHERE constraint_type='UNIQUE' 
                  AND table_schema = DATABASE();

      DECLARE CONTINUE handler for NOT found SET done = true;
        OPEN dropCur;

        read_loop: LOOP
            FETCH dropCur INTO dropCommand;
            IF done THEN
                leave read_loop;
            END IF;

            SET @sdropCommand = dropCommand;

            PREPARE dropClientUpdateKeyStmt FROM @sdropCommand;

            EXECUTE dropClientUpdateKeyStmt;

            DEALLOCATE prepare dropClientUpdateKeyStmt;
        END LOOP;

        CLOSE dropCur;
    END

    ---

    CREATE PROCEDURE st_drop_all_indexes()
    BEGIN
      DECLARE done INT DEFAULT FALSE;
      DECLARE dropCommand VARCHAR(255);
      DECLARE dropCur CURSOR for 
              SELECT DISTINCT concat('ALTER TABLE ', table_schema, '.', table_name, ' DROP INDEX ', index_name, ';')
              FROM information_schema.statistics
              WHERE NON_UNIQUE = 1 AND table_schema = database();

      DECLARE CONTINUE handler for NOT found SET done = true;
        OPEN dropCur;

        read_loop: LOOP
            FETCH dropCur INTO dropCommand;
            IF done THEN
                leave read_loop;
            END IF;

            SET @sdropCommand = dropCommand;

            PREPARE dropClientUpdateKeyStmt FROM @sdropCommand;

            EXECUTE dropClientUpdateKeyStmt;

            DEALLOCATE prepare dropClientUpdateKeyStmt;
        END LOOP;

        CLOSE dropCur;
    END

    ---

    CREATE PROCEDURE st_add_column_if_not_exists(
    IN p_table_name varchar(50), 
    IN p_column_name varchar(50),
    IN p_column_type varchar(50),
    IN p_additional varchar(100),
    OUT p_status_message varchar(100))
        READS SQL DATA
    BEGIN
        DECLARE v_count INT;
        
        # Check wether column exist or not
        SELECT count(*) INTO v_count
        FROM information_schema.columns
        WHERE table_schema = database()
            AND table_name   = p_table_name
            AND column_name  = p_column_name;
            
        IF v_count > 0 THEN
          # Return column already exists message
          SELECT 'Column already Exists' INTO p_status_message;
        ELSE
            # Add Column and return success message
          set @ddl_addcolumn=CONCAT('ALTER TABLE ',database(),'.',p_table_name,
          ' ADD COLUMN ',p_column_name,' ',p_column_type,' ',p_additional);
        prepare add_column_sql from @ddl_addcolumn;
        execute add_column_sql;
          SELECT 'Column Successfully  Created!' INTO p_status_message;
        END IF;
    END

    -- Drop constraints and indexes

    CALL st_drop_all_fkeys();
    CALL st_drop_all_keys();
    CALL st_drop_all_pkeys();
    CALL st_drop_all_indexes(); 

    -- General Tables

    CREATE TABLE IF NOT EXISTS apps  (
      app_id VARCHAR(64) NOT NULL DEFAULT 'public',
      created_at_time BIGINT UNSIGNED
    );

    ALTER TABLE apps
      ADD PRIMARY KEY(app_id);

    INSERT IGNORE INTO apps (app_id, created_at_time) 
      VALUES ('public', 0);

    ------------------------------------------------------------

    CREATE TABLE IF NOT EXISTS tenants (
      app_id VARCHAR(64) NOT NULL DEFAULT 'public',
      tenant_id VARCHAR(64) NOT NULL DEFAULT 'public',
      created_at_time BIGINT UNSIGNED
    );

    ALTER TABLE tenants
      ADD PRIMARY KEY(app_id, tenant_id);

    ALTER TABLE tenants
      ADD FOREIGN KEY (app_id)
        REFERENCES apps (app_id) ON DELETE CASCADE;

    INSERT IGNORE INTO tenants (app_id, tenant_id, created_at_time) 
      VALUES ('public', 'public', 0);

    ------------------------------------------------------------

    CALL st_add_column_if_not_exists('key_value', 'app_id', 'VARCHAR(64)', 'NOT NULL DEFAULT \'public\'', @status_message);
    CALL st_add_column_if_not_exists('key_value', 'tenant_id', 'VARCHAR(64)', 'NOT NULL DEFAULT \'public\'', @status_message);

    ALTER TABLE key_value
      ADD PRIMARY KEY (app_id, tenant_id, name);

    ALTER TABLE key_value
      ADD FOREIGN KEY (app_id, tenant_id)
        REFERENCES tenants (app_id, tenant_id) ON DELETE CASCADE;

    ------------------------------------------------------------

    CREATE TABLE IF NOT EXISTS app_id_to_user_id (
      app_id VARCHAR(64) NOT NULL DEFAULT 'public',
      user_id CHAR(36) NOT NULL,
      recipe_id VARCHAR(128) NOT NULL
    );

    ALTER TABLE app_id_to_user_id
      ADD PRIMARY KEY (app_id, user_id);

    ALTER TABLE app_id_to_user_id
      ADD FOREIGN KEY (app_id)
        REFERENCES apps (app_id) ON DELETE CASCADE;

    INSERT IGNORE INTO app_id_to_user_id (user_id, recipe_id) 
      SELECT user_id, recipe_id
      FROM all_auth_recipe_users;

    ------------------------------------------------------------

    CALL st_add_column_if_not_exists('all_auth_recipe_users', 'app_id', 'VARCHAR(64)', 'NOT NULL DEFAULT \'public\'', @status_message);
    CALL st_add_column_if_not_exists('all_auth_recipe_users', 'tenant_id', 'VARCHAR(64)', 'NOT NULL DEFAULT \'public\'', @status_message);

    ALTER TABLE all_auth_recipe_users
      ADD PRIMARY KEY (app_id, tenant_id, user_id);

    ALTER TABLE all_auth_recipe_users
      ADD FOREIGN KEY (app_id, tenant_id)
        REFERENCES tenants (app_id, tenant_id) ON DELETE CASCADE;

    ALTER TABLE all_auth_recipe_users
      ADD FOREIGN KEY (app_id, user_id)
        REFERENCES app_id_to_user_id (app_id, user_id) ON DELETE CASCADE;

    CREATE INDEX all_auth_recipe_users_pagination_index ON all_auth_recipe_users (time_joined DESC, user_id DESC, tenant_id DESC, app_id DESC);

    -- Multitenancy

    CREATE TABLE IF NOT EXISTS tenant_configs (
      connection_uri_domain VARCHAR(256) DEFAULT '',
      app_id VARCHAR(64) DEFAULT 'public',
      tenant_id VARCHAR(64) DEFAULT 'public',
      core_config TEXT,
      email_password_enabled BOOLEAN,
      passwordless_enabled BOOLEAN,
      third_party_enabled BOOLEAN
    );

    ALTER TABLE tenant_configs
      ADD PRIMARY KEY (connection_uri_domain, app_id, tenant_id);

    ------------------------------------------------------------

    CREATE TABLE IF NOT EXISTS tenant_thirdparty_providers (
      connection_uri_domain VARCHAR(256) DEFAULT '',
      app_id VARCHAR(64) DEFAULT 'public',
      tenant_id VARCHAR(64) DEFAULT 'public',
      third_party_id VARCHAR(28) NOT NULL,
      name VARCHAR(64),
      authorization_endpoint TEXT,
      authorization_endpoint_query_params TEXT,
      token_endpoint TEXT,
      token_endpoint_body_params TEXT,
      user_info_endpoint TEXT,
      user_info_endpoint_query_params TEXT,
      user_info_endpoint_headers TEXT,
      jwks_uri TEXT,
      oidc_discovery_endpoint TEXT,
      require_email BOOLEAN,
      user_info_map_from_id_token_payload_user_id VARCHAR(64),
      user_info_map_from_id_token_payload_email VARCHAR(64),
      user_info_map_from_id_token_payload_email_verified VARCHAR(64),
      user_info_map_from_user_info_endpoint_user_id VARCHAR(64),
      user_info_map_from_user_info_endpoint_email VARCHAR(64),
      user_info_map_from_user_info_endpoint_email_verified VARCHAR(64)
    );

    ALTER TABLE tenant_thirdparty_providers
      ADD PRIMARY KEY (connection_uri_domain, app_id, tenant_id, third_party_id);

    ALTER TABLE tenant_thirdparty_providers
      ADD FOREIGN KEY (connection_uri_domain, app_id, tenant_id)
        REFERENCES tenant_configs (connection_uri_domain, app_id, tenant_id) ON DELETE CASCADE;

    ------------------------------------------------------------

    CREATE TABLE IF NOT EXISTS tenant_thirdparty_provider_clients (
      connection_uri_domain VARCHAR(256) DEFAULT '',
      app_id VARCHAR(64) DEFAULT 'public',
      tenant_id VARCHAR(64) DEFAULT 'public',
      third_party_id VARCHAR(28) NOT NULL,
      client_type VARCHAR(64) NOT NULL DEFAULT '',
      client_id VARCHAR(256) NOT NULL,
      client_secret TEXT,
      scope TEXT,
      force_pkce BOOLEAN,
      additional_config TEXT
    );

    ALTER TABLE tenant_thirdparty_provider_clients
      ADD PRIMARY KEY (connection_uri_domain, app_id, tenant_id, third_party_id, client_type);

    ALTER TABLE tenant_thirdparty_provider_clients
      ADD FOREIGN KEY (connection_uri_domain, app_id, tenant_id, third_party_id)
        REFERENCES tenant_thirdparty_providers (connection_uri_domain, app_id, tenant_id, third_party_id) ON DELETE CASCADE;


    -- Session

    CALL st_add_column_if_not_exists('session_info', 'app_id', 'VARCHAR(64)', 'NOT NULL DEFAULT \'public\'', @status_message);
    CALL st_add_column_if_not_exists('session_info', 'tenant_id', 'VARCHAR(64)', 'NOT NULL DEFAULT \'public\'', @status_message);

    ALTER TABLE session_info
      ADD PRIMARY KEY (app_id, tenant_id, session_handle);

    ALTER TABLE session_info
      ADD FOREIGN KEY (app_id, tenant_id)
        REFERENCES tenants (app_id, tenant_id) ON DELETE CASCADE;

    CREATE INDEX session_expiry_index ON session_info (expires_at);

    ------------------------------------------------------------

    CALL st_add_column_if_not_exists('session_access_token_signing_keys', 'app_id', 'VARCHAR(64)', 'NOT NULL DEFAULT \'public\'', @status_message);

    ALTER TABLE session_access_token_signing_keys
      ADD PRIMARY KEY (app_id, created_at_time);

    ALTER TABLE session_access_token_signing_keys
      ADD FOREIGN KEY (app_id)
        REFERENCES apps (app_id) ON DELETE CASCADE;

    -- JWT

    CALL st_add_column_if_not_exists('jwt_signing_keys', 'app_id', 'VARCHAR(64)', 'NOT NULL DEFAULT \'public\'', @status_message);

    ALTER TABLE jwt_signing_keys
      ADD PRIMARY KEY (app_id, key_id);

    ALTER TABLE jwt_signing_keys
      ADD FOREIGN KEY (app_id)
        REFERENCES apps (app_id) ON DELETE CASCADE;

    -- EmailVerification

    CALL st_add_column_if_not_exists('emailverification_verified_emails', 'app_id', 'VARCHAR(64)', 'NOT NULL DEFAULT \'public\'', @status_message);

    ALTER TABLE emailverification_verified_emails
      ADD PRIMARY KEY (app_id, user_id, email);

    ALTER TABLE emailverification_verified_emails
      ADD FOREIGN KEY (app_id)
        REFERENCES apps (app_id) ON DELETE CASCADE;

    ------------------------------------------------------------

    CALL st_add_column_if_not_exists('emailverification_tokens', 'app_id', 'VARCHAR(64)', 'NOT NULL DEFAULT \'public\'', @status_message);
    CALL st_add_column_if_not_exists('emailverification_tokens', 'tenant_id', 'VARCHAR(64)', 'NOT NULL DEFAULT \'public\'', @status_message);

    ALTER TABLE emailverification_tokens
      ADD PRIMARY KEY (app_id, tenant_id, user_id, email, token);

    ALTER TABLE emailverification_tokens
      ADD FOREIGN KEY (app_id, tenant_id)
        REFERENCES tenants (app_id, tenant_id) ON DELETE CASCADE;

    ALTER TABLE emailverification_tokens
      ADD CONSTRAINT token UNIQUE (token);

    CREATE INDEX emailverification_tokens_index ON emailverification_tokens(token_expiry);

    -- EmailPassword

    CALL st_add_column_if_not_exists('emailpassword_users', 'app_id', 'VARCHAR(64)', 'NOT NULL DEFAULT \'public\'', @status_message);

    ALTER TABLE emailpassword_users
      ADD PRIMARY KEY (app_id, user_id);

    ALTER TABLE emailpassword_users
      ADD FOREIGN KEY (app_id, user_id)
        REFERENCES app_id_to_user_id (app_id, user_id) ON DELETE CASCADE;

    -- ------------------------------------------------------------

    CREATE TABLE IF NOT EXISTS emailpassword_user_to_tenant (
      app_id VARCHAR(64) DEFAULT 'public',
      tenant_id VARCHAR(64) DEFAULT 'public',
      user_id CHAR(36) NOT NULL,
      email VARCHAR(256) NOT NULL
    );

    ALTER TABLE emailpassword_user_to_tenant
      ADD PRIMARY KEY (app_id, tenant_id, user_id);

    ALTER TABLE emailpassword_user_to_tenant
      ADD CONSTRAINT email UNIQUE (app_id, tenant_id, email);

    ALTER TABLE emailpassword_user_to_tenant
      ADD CONSTRAINT FOREIGN KEY (app_id, tenant_id, user_id)
        REFERENCES all_auth_recipe_users (app_id, tenant_id, user_id) ON DELETE CASCADE;

    INSERT IGNORE INTO emailpassword_user_to_tenant (user_id, email)
      SELECT user_id, email FROM emailpassword_users;

    ------------------------------------------------------------

    CALL st_add_column_if_not_exists('emailpassword_pswd_reset_tokens', 'app_id', 'VARCHAR(64)', 'NOT NULL DEFAULT \'public\'', @status_message);

    ALTER TABLE emailpassword_pswd_reset_tokens
      ADD PRIMARY KEY (app_id, user_id, token);

    ALTER TABLE emailpassword_pswd_reset_tokens
      ADD FOREIGN KEY (app_id, user_id)
        REFERENCES emailpassword_users (app_id, user_id) ON DELETE CASCADE;

    ALTER TABLE emailpassword_pswd_reset_tokens
      ADD CONSTRAINT token UNIQUE (token);

    CREATE INDEX emailpassword_password_reset_token_expiry_index ON emailpassword_pswd_reset_tokens (token_expiry);

    -- Passwordless

    CALL st_add_column_if_not_exists('passwordless_users', 'app_id', 'VARCHAR(64)', 'NOT NULL DEFAULT \'public\'', @status_message);

    ALTER TABLE passwordless_users
      ADD PRIMARY KEY (app_id, user_id);

    ALTER TABLE passwordless_users
      ADD FOREIGN KEY (app_id, user_id)
        REFERENCES app_id_to_user_id (app_id, user_id) ON DELETE CASCADE;

    ------------------------------------------------------------

    CREATE TABLE IF NOT EXISTS passwordless_user_to_tenant (
      app_id VARCHAR(64) DEFAULT 'public',
      tenant_id VARCHAR(64) DEFAULT 'public',
      user_id CHAR(36) NOT NULL,
      email VARCHAR(256),
      phone_number VARCHAR(256)
    );

    ALTER TABLE passwordless_user_to_tenant
      ADD PRIMARY KEY (app_id, tenant_id, user_id);

    ALTER TABLE passwordless_user_to_tenant
      ADD CONSTRAINT email UNIQUE (app_id, tenant_id, email);

    ALTER TABLE passwordless_user_to_tenant
      ADD CONSTRAINT phone_number UNIQUE (app_id, tenant_id, phone_number);

    ALTER TABLE passwordless_user_to_tenant
      ADD FOREIGN KEY (app_id, tenant_id, user_id)
        REFERENCES all_auth_recipe_users (app_id, tenant_id, user_id) ON DELETE CASCADE;

    INSERT IGNORE INTO passwordless_user_to_tenant (user_id, email, phone_number)
      SELECT user_id, email, phone_number FROM passwordless_users;

    ------------------------------------------------------------

    CALL st_add_column_if_not_exists('passwordless_devices', 'app_id', 'VARCHAR(64)', 'NOT NULL DEFAULT \'public\'', @status_message);
    CALL st_add_column_if_not_exists('passwordless_devices', 'tenant_id', 'VARCHAR(64)', 'NOT NULL DEFAULT \'public\'', @status_message);

    ALTER TABLE passwordless_devices
      ADD PRIMARY KEY (app_id, tenant_id, device_id_hash);

    ALTER TABLE passwordless_devices
      ADD FOREIGN KEY (app_id, tenant_id)
        REFERENCES tenants (app_id, tenant_id) ON DELETE CASCADE;

    CREATE INDEX passwordless_devices_email_index ON passwordless_devices (app_id, tenant_id, email);

    CREATE INDEX passwordless_devices_phone_number_index ON passwordless_devices (app_id, tenant_id, phone_number);

    ------------------------------------------------------------

    CALL st_add_column_if_not_exists('passwordless_codes', 'app_id', 'VARCHAR(64)', 'NOT NULL DEFAULT \'public\'', @status_message);
    CALL st_add_column_if_not_exists('passwordless_codes', 'tenant_id', 'VARCHAR(64)', 'NOT NULL DEFAULT \'public\'', @status_message);

    ALTER TABLE passwordless_codes
      ADD PRIMARY KEY (app_id, tenant_id, code_id);

    ALTER TABLE passwordless_codes
      ADD FOREIGN KEY (app_id, tenant_id, device_id_hash)
        REFERENCES passwordless_devices (app_id, tenant_id, device_id_hash) ON DELETE CASCADE;

    ALTER TABLE passwordless_codes
      ADD CONSTRAINT link_code_hash
        UNIQUE (app_id, tenant_id, link_code_hash);

    CREATE INDEX passwordless_codes_created_at_index ON passwordless_codes (app_id, tenant_id, created_at);

    -- ThirdParty

    CALL st_add_column_if_not_exists('thirdparty_users', 'app_id', 'VARCHAR(64)', 'NOT NULL DEFAULT \'public\'', @status_message);

    ALTER TABLE thirdparty_users
      ADD PRIMARY KEY (app_id, user_id);

    ALTER TABLE thirdparty_users
      ADD FOREIGN KEY (app_id, user_id)
        REFERENCES app_id_to_user_id (app_id, user_id) ON DELETE CASCADE;

    CREATE INDEX thirdparty_users_thirdparty_user_id_index ON thirdparty_users (app_id, third_party_id, third_party_user_id);

    CREATE INDEX thirdparty_users_email_index ON thirdparty_users (app_id, email);

    ------------------------------------------------------------

    CREATE TABLE IF NOT EXISTS thirdparty_user_to_tenant (
      app_id VARCHAR(64) DEFAULT 'public',
      tenant_id VARCHAR(64) DEFAULT 'public',
      user_id CHAR(36) NOT NULL,
      third_party_id VARCHAR(28) NOT NULL,
      third_party_user_id VARCHAR(256) NOT NULL
    );

    ALTER TABLE thirdparty_user_to_tenant
      ADD PRIMARY KEY (app_id, tenant_id, user_id);

    ALTER TABLE thirdparty_user_to_tenant
      ADD CONSTRAINT third_party_user_id
        UNIQUE (app_id, tenant_id, third_party_id, third_party_user_id);

    ALTER TABLE thirdparty_user_to_tenant
      ADD FOREIGN KEY (app_id, tenant_id, user_id)
        REFERENCES all_auth_recipe_users (app_id, tenant_id, user_id) ON DELETE CASCADE;

    INSERT IGNORE INTO thirdparty_user_to_tenant (user_id, third_party_id, third_party_user_id)
      SELECT user_id, third_party_id, third_party_user_id FROM thirdparty_users;

    -- UserIdMapping

    CALL st_add_column_if_not_exists('userid_mapping', 'app_id', 'VARCHAR(64)', 'NOT NULL DEFAULT \'public\'', @status_message);

    ALTER TABLE userid_mapping
      ADD PRIMARY KEY (app_id, supertokens_user_id, external_user_id);

    ALTER TABLE userid_mapping
      ADD CONSTRAINT supertokens_user_id
        UNIQUE (app_id, supertokens_user_id);

    ALTER TABLE userid_mapping
      ADD CONSTRAINT external_user_id
        UNIQUE (app_id, external_user_id);

    ALTER TABLE userid_mapping
      ADD FOREIGN KEY (app_id, supertokens_user_id)
        REFERENCES app_id_to_user_id (app_id, user_id) ON DELETE CASCADE;

    -- UserRoles

    CALL st_add_column_if_not_exists('roles', 'app_id', 'VARCHAR(64)', 'NOT NULL DEFAULT \'public\'', @status_message);

    ALTER TABLE roles
      ADD PRIMARY KEY (app_id, role);

    ALTER TABLE roles
      ADD FOREIGN KEY (app_id)
        REFERENCES apps (app_id) ON DELETE CASCADE;

    ------------------------------------------------------------

    CALL st_add_column_if_not_exists('role_permissions', 'app_id', 'VARCHAR(64)', 'NOT NULL DEFAULT \'public\'', @status_message);

    ALTER TABLE role_permissions
      ADD PRIMARY KEY (app_id, role, permission);

    ALTER TABLE role_permissions
      ADD FOREIGN KEY (app_id, role)
        REFERENCES roles (app_id, role) ON DELETE CASCADE;

    CREATE INDEX role_permissions_permission_index ON role_permissions (app_id, permission);

    ------------------------------------------------------------

    CALL st_add_column_if_not_exists('user_roles', 'app_id', 'VARCHAR(64)', 'NOT NULL DEFAULT \'public\'', @status_message);
    CALL st_add_column_if_not_exists('user_roles', 'tenant_id', 'VARCHAR(64)', 'NOT NULL DEFAULT \'public\'', @status_message);

    ALTER TABLE user_roles
      ADD PRIMARY KEY (app_id, tenant_id, user_id, role);

    ALTER TABLE user_roles
      ADD FOREIGN KEY (app_id, role)
        REFERENCES roles (app_id, role) ON DELETE CASCADE;

    ALTER TABLE user_roles
      ADD FOREIGN KEY (app_id, tenant_id)
        REFERENCES tenants (app_id, tenant_id) ON DELETE CASCADE;

    CREATE INDEX user_roles_role_index ON user_roles (app_id, tenant_id, role);

    -- UserMetadata

    CALL st_add_column_if_not_exists('user_metadata', 'app_id', 'VARCHAR(64)', 'NOT NULL DEFAULT \'public\'', @status_message);

    ALTER TABLE user_metadata
      ADD PRIMARY KEY (app_id, user_id);

    ALTER TABLE user_metadata
      ADD FOREIGN KEY (app_id)
        REFERENCES apps (app_id) ON DELETE CASCADE;

    -- Dashboard

    CALL st_add_column_if_not_exists('dashboard_users', 'app_id', 'VARCHAR(64)', 'NOT NULL DEFAULT \'public\'', @status_message);

    ALTER TABLE dashboard_users
      ADD PRIMARY KEY (app_id, user_id);

    ALTER TABLE dashboard_users
      ADD CONSTRAINT email
        UNIQUE (app_id, email);

    ALTER TABLE dashboard_users
      ADD FOREIGN KEY (app_id)
        REFERENCES apps (app_id) ON DELETE CASCADE;

    ------------------------------------------------------------

    CALL st_add_column_if_not_exists('dashboard_user_sessions', 'app_id', 'VARCHAR(64)', 'NOT NULL DEFAULT \'public\'', @status_message);

    ALTER TABLE dashboard_user_sessions
      ADD PRIMARY KEY (app_id, session_id);

    ALTER TABLE dashboard_user_sessions
      ADD FOREIGN KEY (app_id, user_id)
        REFERENCES dashboard_users (app_id, user_id) ON DELETE CASCADE;

    CREATE INDEX dashboard_user_sessions_expiry_index ON dashboard_user_sessions (expiry);

    -- TOTP

    CALL st_add_column_if_not_exists('totp_users', 'app_id', 'VARCHAR(64)', 'NOT NULL DEFAULT \'public\'', @status_message);

    ALTER TABLE totp_users
      ADD PRIMARY KEY (app_id, user_id);

    ALTER TABLE totp_users
      ADD FOREIGN KEY (app_id)
        REFERENCES apps (app_id) ON DELETE CASCADE;

    ------------------------------------------------------------

    CALL st_add_column_if_not_exists('totp_user_devices', 'app_id', 'VARCHAR(64)', 'NOT NULL DEFAULT \'public\'', @status_message);

    ALTER TABLE totp_user_devices
      ADD PRIMARY KEY (app_id, user_id, device_name);

    ALTER TABLE totp_user_devices
      ADD FOREIGN KEY (app_id, user_id)
        REFERENCES totp_users (app_id, user_id) ON DELETE CASCADE;

    ------------------------------------------------------------

    CALL st_add_column_if_not_exists('totp_used_codes', 'app_id', 'VARCHAR(64)', 'NOT NULL DEFAULT \'public\'', @status_message);
    CALL st_add_column_if_not_exists('totp_used_codes', 'tenant_id', 'VARCHAR(64)', 'NOT NULL DEFAULT \'public\'', @status_message);

    ALTER TABLE totp_used_codes
      ADD PRIMARY KEY (app_id, tenant_id, user_id, created_time_ms);

    ALTER TABLE totp_used_codes
      ADD FOREIGN KEY (app_id, user_id)
        REFERENCES totp_users (app_id, user_id) ON DELETE CASCADE;

    ALTER TABLE totp_used_codes
      ADD FOREIGN KEY (app_id, tenant_id)
        REFERENCES tenants (app_id, tenant_id) ON DELETE CASCADE;

    CREATE INDEX totp_used_codes_expiry_time_ms_index ON totp_used_codes (app_id, tenant_id, expiry_time_ms);

    -- ActiveUsers

    CALL st_add_column_if_not_exists('user_last_active', 'app_id', 'VARCHAR(64)', 'NOT NULL DEFAULT \'public\'', @status_message);

    ALTER TABLE user_last_active
      ADD PRIMARY KEY (app_id, user_id);

    ALTER TABLE user_last_active
      ADD FOREIGN KEY (app_id)
        REFERENCES apps (app_id) ON DELETE CASCADE;

    -- Drop procedures

    DROP PROCEDURE st_drop_all_fkeys;

    DROP PROCEDURE st_drop_all_keys;

    DROP PROCEDURE st_drop_all_pkeys;

    DROP PROCEDURE st_drop_all_indexes;

    DROP PROCEDURE st_add_column_if_not_exists;
    ```

4. Start the new instance(s) of the core (version 6.0.0)

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
