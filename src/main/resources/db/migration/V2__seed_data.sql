-- Demonstracijai skirti prasmingi duomenys: trys grupes, ju uzduotys ir progreso irasai.

insert into guild (name, description)
values ('Thursday Night Crew',
        'Ketvirtadieniais renkasi kampanija - keturi zaidejai ir vienas DM.'),
       ('Iron Vow',
        'Sporto ir sveikatos ipirociu grupe, pastoviai augina STR ir CON.'),
       ('The Quiet Library',
        'Skaitymo ir mokymosi grupe - daugiausia INT ir WIS uzduotys.');

insert into quest (guild_id, title, description, stat, type, difficulty, target_count, active)
values
    -- Thursday Night Crew
    ((select id from guild where name = 'Thursday Night Crew'),
     'Finish the campaign notes', 'Uzrasyti praejusios sesijos ivykius iki kito susitikimo.',
     'INT', 'MILESTONE', 'EASY', null, true),
    ((select id from guild where name = 'Thursday Night Crew'),
     'Call a friend you have not seen in a month', 'Tikras pokalbis, ne zinute.',
     'CHA', 'HABIT', 'MEDIUM', null, true),
    ((select id from guild where name = 'Thursday Night Crew'),
     'Brew coffee for the whole table', 'DM skirta uzduotis uz netiketa geranoriskuma.',
     'CHA', 'SIDE_QUEST', 'TRIVIAL', null, true),

    -- Iron Vow
    ((select id from guild where name = 'Iron Vow'),
     'Run 50 kilometers', 'Kaupiamoji uzduotis - kiekvienas patvirtintas irasas priartina prie tikslo.',
     'CON', 'GRIND', 'HARD', 50, true),
    ((select id from guild where name = 'Iron Vow'),
     'Morning mobility routine', 'Penkiolika minuciu tempimo pries darba.',
     'DEX', 'HABIT', 'TRIVIAL', null, true),
    ((select id from guild where name = 'Iron Vow'),
     'Deadlift bodyweight x2', 'Vienkartinis jegos rodiklis.',
     'STR', 'MILESTONE', 'LEGENDARY', null, true),
    ((select id from guild where name = 'Iron Vow'),
     'Winter swim challenge', 'Sezonas baigesi - uzduotis nebeaktyvi.',
     'CON', 'MILESTONE', 'HARD', null, false),

    -- The Quiet Library
    ((select id from guild where name = 'The Quiet Library'),
     'Read 12 books this year', 'Po knyga per menesi.',
     'INT', 'GRIND', 'MEDIUM', 12, true),
    ((select id from guild where name = 'The Quiet Library'),
     'Journal before sleeping', 'Trys sakiniai apie diena.',
     'WIS', 'HABIT', 'TRIVIAL', null, true);

-- Patvirtinti irasai (XP uzsaldytas patvirtinimo metu), atmesti ir dar neperziureti.
insert into log_entry (quest_id, note, evidence_url, status, xp_awarded, inspiration_awarded,
                       reviewer_note, occurred_at, reviewed_at)
values
    ((select id from quest where title = 'Finish the campaign notes'),
     'Surasiau visa Vaiduokliu pilies epizoda, 3 puslapiai.',
     'https://example.org/evidence/campaign-notes.jpg',
     'APPROVED', 20, 0, 'Puikus santrauka, visi vardai teisingi.',
     timestamptz '2026-09-04 19:40:00+03', timestamptz '2026-09-05 09:12:00+03'),

    ((select id from quest where title = 'Call a friend you have not seen in a month'),
     'Paskambinau Ugnei, kalbejome 40 minuciu.', null,
     'APPROVED', 30, 1, 'Skiriu heroic inspiration - buvo sunkus skambutis.',
     timestamptz '2026-09-07 20:05:00+03', timestamptz '2026-09-08 08:00:00+03'),

    ((select id from quest where title = 'Brew coffee for the whole table'),
     'Atnesiau kava visai komandai pries sesija.',
     'https://example.org/evidence/coffee.jpg',
     'APPROVED', 10, 1, 'Netiketa ir labai laiku.',
     timestamptz '2026-09-11 18:30:00+03', timestamptz '2026-09-11 22:00:00+03'),

    ((select id from quest where title = 'Run 50 kilometers'),
     'Begau 8 km palei upe.',
     'https://example.org/evidence/run-8km.png',
     'APPROVED', 50, 0, null,
     timestamptz '2026-09-09 07:15:00+03', timestamptz '2026-09-09 19:00:00+03'),

    ((select id from quest where title = 'Run 50 kilometers'),
     'Dar 12 km savaitgali.',
     'https://example.org/evidence/run-12km.png',
     'APPROVED', 50, 0, 'Uzskaitau, nors nuotrauka neryski.',
     timestamptz '2026-09-13 10:00:00+03', timestamptz '2026-09-14 11:30:00+03'),

    ((select id from quest where title = 'Run 50 kilometers'),
     'Begau 5 km, bet laikrodis neissaugojo.', null,
     'REJECTED', 0, 0, 'Reikia irodymo - be jo neuzskaitau.',
     timestamptz '2026-09-16 06:50:00+03', timestamptz '2026-09-16 20:10:00+03'),

    ((select id from quest where title = 'Run 50 kilometers'),
     'Vakarykstis 10 km begimas.',
     'https://example.org/evidence/run-10km.png',
     'PENDING', 0, 0, null,
     timestamptz '2026-09-19 18:20:00+03', null),

    ((select id from quest where title = 'Morning mobility routine'),
     'Penkios dienos is eiles.', null,
     'APPROVED', 10, 0, null,
     timestamptz '2026-09-15 07:00:00+03', timestamptz '2026-09-15 21:00:00+03'),

    ((select id from quest where title = 'Deadlift bodyweight x2'),
     'Pakeliau 130 kg prie 65 kg svorio.',
     'https://example.org/evidence/deadlift.mp4',
     'PENDING', 0, 0, null,
     timestamptz '2026-09-20 17:45:00+03', null),

    ((select id from quest where title = 'Read 12 books this year'),
     'Perskaiciau "The Dispossessed".',
     'https://example.org/evidence/book-cover.jpg',
     'APPROVED', 30, 0, null,
     timestamptz '2026-08-28 22:00:00+03', timestamptz '2026-08-29 09:00:00+03'),

    ((select id from quest where title = 'Read 12 books this year'),
     'Perskaiciau "Piranesi".', null,
     'PENDING', 0, 0, null,
     timestamptz '2026-09-18 23:30:00+03', null),

    ((select id from quest where title = 'Journal before sleeping'),
     'Septynios dienos be praleidimo.',
     'https://example.org/evidence/journal.jpg',
     'APPROVED', 10, 1, 'Serija nenutrukusi - skiriu ikvepima.',
     timestamptz '2026-09-17 23:10:00+03', timestamptz '2026-09-18 07:30:00+03');
