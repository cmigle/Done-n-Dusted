-- Draugystes: naudotojas siuncia kvietima, gavejas ji priima (ACCEPTED) arba uzblokuoja (BLOCKED).
-- Atsaukimas, atmetimas, draugystes nutraukimas ir atblokavimas - eilutes istrynimas.
-- BLOCKED busenoje requester_id - tas, kuris uzblokavo (tik jis gali atblokuoti).

create table friendship (
    id           bigserial primary key,
    requester_id bigint      not null references app_user (id) on delete cascade,
    addressee_id bigint      not null references app_user (id) on delete cascade,
    status       varchar(10) not null default 'PENDING',
    created_at   timestamptz not null default now(),
    responded_at timestamptz,
    constraint ck_friendship_status check (status in ('PENDING', 'ACCEPTED', 'BLOCKED')),
    constraint ck_friendship_not_self check (requester_id <> addressee_id),
    -- atsakymo laikas yra tada ir tik tada, kai kvietimas nebelaukia
    constraint ck_friendship_responded_at check ((status = 'PENDING') = (responded_at is null))
);

-- Viena eilute porai nepriklausomai nuo krypties: A -> B ir B -> A yra ta pati pora.
create unique index uq_friendship_pair
    on friendship (least(requester_id, addressee_id), greatest(requester_id, addressee_id));
create index idx_friendship_requester on friendship (requester_id);
create index idx_friendship_addressee on friendship (addressee_id);

-- Seed: ieva ir jonas - draugai, rokas pakviete ieva ir laukia atsakymo.
insert into friendship (requester_id, addressee_id, status, responded_at)
select r.id, a.id, v.status, case when v.status = 'PENDING' then null else now() end
from (values ('ieva', 'jonas', 'ACCEPTED'),
             ('rokas', 'ieva', 'PENDING')) as v (requester, addressee, status)
         join app_user r on r.username = v.requester
         join app_user a on a.username = v.addressee;
