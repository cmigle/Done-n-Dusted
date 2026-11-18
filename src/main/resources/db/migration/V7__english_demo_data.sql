-- Translate the Lithuanian demo texts from V2 to English.
-- Rows are matched by guild name or quest title and by their original text,
-- so data that users created or edited stays unchanged.

update guild g
set description = v.new_text
from (values ('Thursday Night Crew',
              'Ketvirtadieniais renkasi kampanija - keturi zaidejai ir vienas DM.',
              'A campaign group that meets on Thursdays: four players and one game master.'),
             ('Iron Vow',
              'Sporto ir sveikatos ipirociu grupe, pastoviai augina STR ir CON.',
              'A guild for sport and health habits, steadily raising STR and CON.'),
             ('The Quiet Library',
              'Skaitymo ir mokymosi grupe - daugiausia INT ir WIS uzduotys.',
              'A guild for reading and learning, mostly INT and WIS quests.')) as v (name, old_text, new_text)
where g.name = v.name
  and g.description = v.old_text;

update quest q
set description = v.new_text
from (values ('Finish the campaign notes',
              'Uzrasyti praejusios sesijos ivykius iki kito susitikimo.',
              'Write up the last session''s events before the next meeting.'),
             ('Call a friend you have not seen in a month',
              'Tikras pokalbis, ne zinute.',
              'A real conversation, not a text message.'),
             ('Brew coffee for the whole table',
              'DM skirta uzduotis uz netiketa geranoriskuma.',
              'A quest from the game master for unexpected kindness.'),
             ('Run 50 kilometers',
              'Kaupiamoji uzduotis - kiekvienas patvirtintas irasas priartina prie tikslo.',
              'A cumulative quest: every approved log entry brings you closer to the goal.'),
             ('Morning mobility routine',
              'Penkiolika minuciu tempimo pries darba.',
              'Fifteen minutes of stretching before work.'),
             ('Deadlift bodyweight x2',
              'Vienkartinis jegos rodiklis.',
              'A one-off strength milestone.'),
             ('Winter swim challenge',
              'Sezonas baigesi - uzduotis nebeaktyvi.',
              'The season is over, so this quest is no longer active.'),
             ('Read 12 books this year',
              'Po knyga per menesi.',
              'One book a month.'),
             ('Journal before sleeping',
              'Trys sakiniai apie diena.',
              'Three sentences about the day.')) as v (title, old_text, new_text)
where q.title = v.title
  and q.description = v.old_text;

update log_entry l
set note = v.new_text
from (values ('Finish the campaign notes',
              'Surasiau visa Vaiduokliu pilies epizoda, 3 puslapiai.',
              'Wrote up the whole Ghost Castle episode, 3 pages.'),
             ('Call a friend you have not seen in a month',
              'Paskambinau Ugnei, kalbejome 40 minuciu.',
              'Called Ugne and we talked for 40 minutes.'),
             ('Brew coffee for the whole table',
              'Atnesiau kava visai komandai pries sesija.',
              'Brought coffee for the whole team before the session.'),
             ('Run 50 kilometers',
              'Begau 8 km palei upe.',
              'Ran 8 km along the river.'),
             ('Run 50 kilometers',
              'Dar 12 km savaitgali.',
              'Another 12 km over the weekend.'),
             ('Run 50 kilometers',
              'Begau 5 km, bet laikrodis neissaugojo.',
              'Ran 5 km, but my watch didn''t save it.'),
             ('Run 50 kilometers',
              'Vakarykstis 10 km begimas.',
              'Yesterday''s 10 km run.'),
             ('Morning mobility routine',
              'Penkios dienos is eiles.',
              'Five days in a row.'),
             ('Deadlift bodyweight x2',
              'Pakeliau 130 kg prie 65 kg svorio.',
              'Lifted 130 kg at 65 kg bodyweight.'),
             ('Read 12 books this year',
              'Perskaiciau "The Dispossessed".',
              'Finished reading "The Dispossessed".'),
             ('Read 12 books this year',
              'Perskaiciau "Piranesi".',
              'Finished reading "Piranesi".'),
             ('Journal before sleeping',
              'Septynios dienos be praleidimo.',
              'Seven days without skipping one.')) as v (title, old_text, new_text)
         join quest q on q.title = v.title
where l.quest_id = q.id
  and l.note = v.old_text;

update log_entry l
set reviewer_note = v.new_text
from (values ('Finish the campaign notes',
              'Puikus santrauka, visi vardai teisingi.',
              'Great summary, all the names are right.'),
             ('Call a friend you have not seen in a month',
              'Skiriu heroic inspiration - buvo sunkus skambutis.',
              'Awarding heroic inspiration: that call was not easy.'),
             ('Brew coffee for the whole table',
              'Netiketa ir labai laiku.',
              'Unexpected and perfectly timed.'),
             ('Run 50 kilometers',
              'Uzskaitau, nors nuotrauka neryski.',
              'Approved, even though the photo is blurry.'),
             ('Run 50 kilometers',
              'Reikia irodymo - be jo neuzskaitau.',
              'Evidence needed: without it, this can''t count.'),
             ('Journal before sleeping',
              'Serija nenutrukusi - skiriu ikvepima.',
              'Unbroken streak: awarding inspiration.')) as v (title, old_text, new_text)
         join quest q on q.title = v.title
where l.quest_id = q.id
  and l.reviewer_note = v.old_text;
