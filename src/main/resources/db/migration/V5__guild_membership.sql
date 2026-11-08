-- LD2: kas kuriai grupei priklauso ir kas ka sukure.
-- Naudotojas grupeje turi role PLAYER arba DUNGEON_MASTER; skirtingose grupese - skirtingas roles.

create table guild_membership (
    id        bigserial primary key,
    guild_id  bigint      not null references guild (id) on delete cascade,
    user_id   bigint      not null references app_user (id) on delete cascade,
    role      varchar(20) not null,
    joined_at timestamptz not null default now(),
    constraint uq_guild_membership unique (guild_id, user_id),
    constraint ck_guild_membership_role check (role in ('PLAYER', 'DUNGEON_MASTER'))
);

-- Grupeje daugiausia vienas DM: unikalus indeksas galioja tik DM eilutems.
create unique index uq_guild_one_dm on guild_membership (guild_id) where role = 'DUNGEON_MASTER';
create index idx_guild_membership_user on guild_membership (user_id);

alter table guild add column owner_id bigint references app_user (id);
alter table quest add column created_by bigint references app_user (id) on delete set null;
alter table log_entry add column author_id bigint references app_user (id) on delete cascade;
alter table log_entry add column reviewer_id bigint references app_user (id) on delete set null;

-- Seed: rokas - DM dviejose grupese ir zaidejas bibliotekoje, ieva - atvirksciai, jonas - tik zaidejas.
update guild g
set owner_id = u.id
from (values ('Thursday Night Crew', 'rokas'),
             ('Iron Vow', 'rokas'),
             ('The Quiet Library', 'ieva')) as v (guild, username)
         join app_user u on u.username = v.username
where g.name = v.guild;

insert into guild_membership (guild_id, user_id, role)
select g.id, u.id, v.role
from (values ('Thursday Night Crew', 'rokas', 'DUNGEON_MASTER'),
             ('Thursday Night Crew', 'jonas', 'PLAYER'),
             ('Thursday Night Crew', 'ieva', 'PLAYER'),
             ('Iron Vow', 'rokas', 'DUNGEON_MASTER'),
             ('Iron Vow', 'jonas', 'PLAYER'),
             ('Iron Vow', 'ieva', 'PLAYER'),
             ('The Quiet Library', 'ieva', 'DUNGEON_MASTER'),
             ('The Quiet Library', 'rokas', 'PLAYER')) as v (guild, username, role)
         join guild g on g.name = v.guild
         join app_user u on u.username = v.username;

update quest q
set created_by = u.id
from (values ('Finish the campaign notes', 'rokas'),
             ('Call a friend you have not seen in a month', 'jonas'),
             ('Brew coffee for the whole table', 'rokas'),
             ('Run 50 kilometers', 'jonas'),
             ('Morning mobility routine', 'ieva'),
             ('Deadlift bodyweight x2', 'jonas'),
             ('Winter swim challenge', 'rokas'),
             ('Read 12 books this year', 'ieva'),
             ('Journal before sleeping', 'rokas')) as v (title, username)
         join app_user u on u.username = v.username
where q.title = v.title;

-- Perziurejo tos grupes DM; savo irasu niekas netvirtino.
update log_entry l
set author_id   = a.id,
    reviewer_id = r.id
from (values ('Surasiau visa Vaiduokliu pilies epizoda, 3 puslapiai.', 'jonas', 'rokas'),
             ('Paskambinau Ugnei, kalbejome 40 minuciu.', 'jonas', 'rokas'),
             ('Atnesiau kava visai komandai pries sesija.', 'ieva', 'rokas'),
             ('Begau 8 km palei upe.', 'jonas', 'rokas'),
             ('Dar 12 km savaitgali.', 'jonas', 'rokas'),
             ('Begau 5 km, bet laikrodis neissaugojo.', 'ieva', 'rokas'),
             ('Vakarykstis 10 km begimas.', 'jonas', null),
             ('Penkios dienos is eiles.', 'ieva', 'rokas'),
             ('Pakeliau 130 kg prie 65 kg svorio.', 'jonas', null),
             ('Perskaiciau "The Dispossessed".', 'rokas', 'ieva'),
             ('Perskaiciau "Piranesi".', 'rokas', null),
             ('Septynios dienos be praleidimo.', 'rokas', 'ieva')) as v (note, author, reviewer)
         join app_user a on a.username = v.author
         left join app_user r on r.username = v.reviewer
where l.note = v.note;

alter table guild alter column owner_id set not null;
alter table log_entry alter column author_id set not null;
alter table log_entry add constraint ck_log_pending_no_reviewer check (status <> 'PENDING' or reviewer_id is null);

create index idx_log_entry_author on log_entry (author_id);
