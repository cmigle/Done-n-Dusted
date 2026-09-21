create table guild (
    id          bigserial primary key,
    name        varchar(60) not null,
    description varchar(500),
    created_at  timestamptz not null default now(),
    constraint uq_guild_name unique (name)
);

create table quest (
    id           bigserial primary key,
    guild_id     bigint      not null references guild (id) on delete cascade,
    title        varchar(120) not null,
    description  varchar(1000),
    stat         varchar(3)  not null,
    type         varchar(20) not null,
    difficulty   varchar(20) not null,
    target_count integer,
    active       boolean     not null default true,
    created_at   timestamptz not null default now(),
    constraint uq_quest_guild_title unique (guild_id, title),
    constraint ck_quest_stat check (stat in ('STR', 'DEX', 'CON', 'INT', 'WIS', 'CHA')),
    constraint ck_quest_type check (type in ('MILESTONE', 'HABIT', 'GRIND', 'SIDE_QUEST')),
    constraint ck_quest_difficulty check (difficulty in ('TRIVIAL', 'EASY', 'MEDIUM', 'HARD', 'LEGENDARY')),
    constraint ck_quest_target_count check (target_count is null or target_count > 0)
);

create index idx_quest_guild on quest (guild_id);

create table log_entry (
    id                  bigserial primary key,
    quest_id            bigint      not null references quest (id) on delete cascade,
    note                varchar(1000),
    evidence_url        varchar(500),
    status              varchar(10) not null default 'PENDING',
    xp_awarded          integer     not null default 0,
    inspiration_awarded integer     not null default 0,
    reviewer_note       varchar(500),
    occurred_at         timestamptz not null,
    reviewed_at         timestamptz,
    created_at          timestamptz not null default now(),
    constraint ck_log_status check (status in ('PENDING', 'APPROVED', 'REJECTED')),
    constraint ck_log_xp check (xp_awarded >= 0),
    constraint ck_log_inspiration check (inspiration_awarded >= 0),
    -- neapdorotas irasas negali tureti nei tasku, nei ikvepimo, nei perziuros laiko
    constraint ck_log_pending_is_empty check (
        status <> 'PENDING' or (xp_awarded = 0 and inspiration_awarded = 0 and reviewed_at is null)
    ),
    constraint ck_log_reviewed_at check (status = 'PENDING' or reviewed_at is not null)
);

create index idx_log_entry_quest on log_entry (quest_id);
create index idx_log_entry_status on log_entry (status);
