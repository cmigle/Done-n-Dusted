-- LD2: naudotojai ir refresh zetonai.
-- Slaptazodziai saugomi tik kaip BCrypt maisa (su druska), niekada atviru tekstu.

create table app_user (
    id            bigserial primary key,
    username      varchar(30)  not null,
    password_hash varchar(100) not null,
    role          varchar(10)  not null default 'USER',
    enabled       boolean      not null default true,
    created_at    timestamptz  not null default now(),
    constraint ck_app_user_role check (role in ('USER', 'ADMIN'))
);

-- Vardas unikalus neatsizvelgiant i raidziu dydi: "Jonas" ir "jonas" - tas pats naudotojas.
create unique index uq_app_user_username on app_user (lower(username));

-- Refresh zetonas DB saugomas tik kaip SHA-256 maisa: nutekejus DB, zetonu panaudoti nepavyks.
create table refresh_token (
    id         bigserial primary key,
    user_id    bigint      not null references app_user (id) on delete cascade,
    token_hash varchar(64) not null,
    expires_at timestamptz not null,
    revoked_at timestamptz,
    created_at timestamptz not null default now(),
    constraint uq_refresh_token_hash unique (token_hash)
);

create index idx_refresh_token_user on refresh_token (user_id);

-- Demonstraciniai naudotojai. Visu slaptazodis: Demo123!
insert into app_user (username, password_hash, role)
values ('admin', '$2a$10$d/WttS0e1/Z.m9oXb7DC9eOb8TUKi8i2CHNsHi/Ya9JK0sWu1s2Ry', 'ADMIN'),
       ('rokas', '$2a$10$ibIR7CyF6seORDmDAh.ZRusWzHAHc3M72PnRAfFUaVBM5qQA5Grx.', 'USER'),
       ('ieva',  '$2a$10$gF7a6vMNvwoj8njuACUvV.IoZwyeQ/3.F0okfK.8phNwbjwAn6WL.', 'USER'),
       ('jonas', '$2a$10$KwKg9zaD9G5pu0pR8Vit0.1vRZ7q7wLVmXc4mMqm5gZbmE1zR3trq', 'USER');
