-- Baseline schema for EFTService, generated from the JPA entities.
-- Schema changes go in new V2__..., V3__... files; never edit an applied migration.

CREATE TABLE public.external_accounts (
    currency character varying(3) NOT NULL,
    institution_number character varying(3) NOT NULL,
    transit_number character varying(5) NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    updated_at timestamp(6) with time zone NOT NULL,
    account_number character varying(12) NOT NULL,
    id uuid NOT NULL,
    status character varying(24) NOT NULL,
    account_holder_name character varying(128) NOT NULL,
    customer_id character varying(255) NOT NULL,
    CONSTRAINT external_accounts_status_check CHECK (((status)::text = ANY ((ARRAY['PENDING_VERIFICATION'::character varying, 'ACTIVE'::character varying, 'DISABLED'::character varying])::text[])))
);

ALTER TABLE ONLY public.external_accounts
    ADD CONSTRAINT external_accounts_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.external_accounts
    ADD CONSTRAINT uk_extacct_owner_routing UNIQUE (customer_id, institution_number, transit_number, account_number);

CREATE INDEX idx_extacct_customer ON public.external_accounts USING btree (customer_id);

