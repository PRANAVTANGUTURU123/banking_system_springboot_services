-- Runs once, when the postgres container's data volume is first created.
-- One database per service (database-per-service); tables come from each service's Flyway migrations.
CREATE DATABASE accountsdb;
CREATE DATABASE customerdb;
CREATE DATABASE billerdb;
CREATE DATABASE paymentdb;
CREATE DATABASE billpayworkerdb;
CREATE DATABASE settlementdb;

CREATE ROLE eft LOGIN PASSWORD 'eft';
CREATE DATABASE eftdb OWNER eft;

CREATE ROLE eftworker LOGIN PASSWORD 'eftworker';
CREATE DATABASE eftworkerdb OWNER eftworker;
