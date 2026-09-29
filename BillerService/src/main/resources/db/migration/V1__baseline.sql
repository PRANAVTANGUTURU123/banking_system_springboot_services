-- Baseline schema for BillerService, generated from the JPA entities.
-- Schema changes go in new V2__..., V3__... files; never edit an applied migration.

CREATE TABLE public.billers (
    created_at timestamp(6) with time zone NOT NULL,
    updated_at timestamp(6) with time zone NOT NULL,
    id uuid NOT NULL,
    category character varying(255) NOT NULL,
    customer_id character varying(255) NOT NULL,
    name character varying(255) NOT NULL,
    reference_number character varying(255) NOT NULL,
    status character varying(255) NOT NULL
);

ALTER TABLE ONLY public.billers
    ADD CONSTRAINT billers_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.billers
    ADD CONSTRAINT uk_biller_owner_ref UNIQUE (customer_id, reference_number);

CREATE INDEX idx_biller_customer ON public.billers USING btree (customer_id);

