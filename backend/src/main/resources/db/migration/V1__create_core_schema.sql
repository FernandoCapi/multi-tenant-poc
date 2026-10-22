-- Core schema (docs/modelo-de-dominio.md, ADR-0001).
-- Every business table carries family_id. References between business tables use composite
-- keys that include family_id, so a row can never point to a row of another family:
-- foreign key checks are not subject to row-level security, so this is the database guard
-- against cross-family references.

CREATE TABLE families (
    id         uuid         NOT NULL DEFAULT gen_random_uuid(),
    name       varchar(120) NOT NULL,
    created_at timestamptz  NOT NULL DEFAULT now(),
    CONSTRAINT families_pk PRIMARY KEY (id)
);

CREATE TABLE users (
    id           uuid         NOT NULL DEFAULT gen_random_uuid(),
    family_id    uuid         NOT NULL,
    email        varchar(254) NOT NULL,
    display_name varchar(120) NOT NULL,
    created_at   timestamptz  NOT NULL DEFAULT now(),
    CONSTRAINT users_pk PRIMARY KEY (id),
    CONSTRAINT users_family_id_id_uk UNIQUE (family_id, id),
    CONSTRAINT users_family_fk FOREIGN KEY (family_id) REFERENCES families (id)
);

-- Login e-mail is unique across the whole system (not per family)
CREATE UNIQUE INDEX users_email_uk ON users (lower(email));

CREATE TABLE goals (
    id            uuid          NOT NULL DEFAULT gen_random_uuid(),
    family_id     uuid          NOT NULL,
    title         varchar(120)  NOT NULL,
    description   varchar(1000),
    target_amount numeric(19, 2) NOT NULL,
    currency      varchar(3)    NOT NULL DEFAULT 'BRL',
    due_date      date,
    status        varchar(16)   NOT NULL DEFAULT 'ACTIVE',
    created_by    uuid          NOT NULL,
    created_at    timestamptz   NOT NULL DEFAULT now(),
    completed_at  timestamptz,
    archived_at   timestamptz,
    CONSTRAINT goals_pk PRIMARY KEY (id),
    CONSTRAINT goals_family_id_id_uk UNIQUE (family_id, id),
    CONSTRAINT goals_family_fk FOREIGN KEY (family_id) REFERENCES families (id),
    CONSTRAINT goals_created_by_fk FOREIGN KEY (family_id, created_by) REFERENCES users (family_id, id),
    CONSTRAINT goals_title_ck CHECK (btrim(title) <> ''),
    CONSTRAINT goals_target_amount_ck CHECK (target_amount > 0),
    CONSTRAINT goals_currency_ck CHECK (currency = 'BRL'),
    CONSTRAINT goals_status_ck CHECK (status IN ('ACTIVE', 'COMPLETED', 'ARCHIVED'))
);

CREATE INDEX goals_family_status_idx ON goals (family_id, status);

CREATE TABLE goal_members (
    family_id   uuid        NOT NULL,
    goal_id     uuid        NOT NULL,
    user_id     uuid        NOT NULL,
    role        varchar(16) NOT NULL,
    contributes boolean     NOT NULL DEFAULT true,
    added_by    uuid        NOT NULL,
    added_at    timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT goal_members_pk PRIMARY KEY (family_id, goal_id, user_id),
    -- Memberships go away with the goal; goals with contributions cannot be deleted (INV-11)
    CONSTRAINT goal_members_goal_fk FOREIGN KEY (family_id, goal_id)
        REFERENCES goals (family_id, id) ON DELETE CASCADE,
    CONSTRAINT goal_members_user_fk FOREIGN KEY (family_id, user_id) REFERENCES users (family_id, id),
    CONSTRAINT goal_members_added_by_fk FOREIGN KEY (family_id, added_by) REFERENCES users (family_id, id),
    CONSTRAINT goal_members_role_ck CHECK (role IN ('OWNER', 'MEMBER'))
);

-- "Goals I can see" lookups
CREATE INDEX goal_members_family_user_idx ON goal_members (family_id, user_id);

CREATE TABLE contributions (
    id               uuid           NOT NULL DEFAULT gen_random_uuid(),
    family_id        uuid           NOT NULL,
    goal_id          uuid           NOT NULL,
    -- References users, not goal_members: a contribution outlives the membership (INV-07)
    contributor_id   uuid           NOT NULL,
    registered_by_id uuid           NOT NULL,
    amount           numeric(19, 2) NOT NULL,
    contributed_on   date           NOT NULL,
    note             varchar(500),
    created_at       timestamptz    NOT NULL DEFAULT now(),
    CONSTRAINT contributions_pk PRIMARY KEY (id),
    CONSTRAINT contributions_family_id_id_uk UNIQUE (family_id, id),
    -- No cascade: a goal that ever had a contribution cannot be deleted (INV-11)
    CONSTRAINT contributions_goal_fk FOREIGN KEY (family_id, goal_id) REFERENCES goals (family_id, id),
    CONSTRAINT contributions_contributor_fk FOREIGN KEY (family_id, contributor_id)
        REFERENCES users (family_id, id),
    CONSTRAINT contributions_registered_by_fk FOREIGN KEY (family_id, registered_by_id)
        REFERENCES users (family_id, id),
    CONSTRAINT contributions_amount_ck CHECK (amount > 0)
);

CREATE INDEX contributions_family_goal_idx ON contributions (family_id, goal_id);
CREATE INDEX contributions_family_contributor_idx ON contributions (family_id, contributor_id);

CREATE TABLE contribution_reversals (
    id              uuid         NOT NULL DEFAULT gen_random_uuid(),
    family_id       uuid         NOT NULL,
    contribution_id uuid         NOT NULL,
    reversed_by_id  uuid         NOT NULL,
    reason          varchar(500) NOT NULL,
    reversed_at     timestamptz  NOT NULL DEFAULT now(),
    CONSTRAINT contribution_reversals_pk PRIMARY KEY (id),
    -- At most one reversal per contribution (D-13)
    CONSTRAINT contribution_reversals_contribution_uk UNIQUE (family_id, contribution_id),
    CONSTRAINT contribution_reversals_contribution_fk FOREIGN KEY (family_id, contribution_id)
        REFERENCES contributions (family_id, id),
    CONSTRAINT contribution_reversals_reversed_by_fk FOREIGN KEY (family_id, reversed_by_id)
        REFERENCES users (family_id, id),
    CONSTRAINT contribution_reversals_reason_ck CHECK (btrim(reason) <> '')
);
