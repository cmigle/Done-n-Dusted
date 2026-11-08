-- LD2 demonstracija: ka DB saugo apie naudotojus, roles ir zetonus.

\echo
\echo '== Naudotojai: slaptazodis saugomas tik kaip BCrypt maisa ($2a$10$ + druska + maisa)'
SELECT id, username, role, enabled, left(password_hash, 29) || '...' AS password_hash
FROM app_user ORDER BY id;

\echo '== Narystes: role kiekvienoje grupeje atskirai (PLAYER / DUNGEON_MASTER)'
SELECT g.id AS guild_id, g.name AS grupe, u.username, m.role,
       CASE WHEN g.owner_id = u.id THEN 'savininkas' ELSE '' END AS savininkas
FROM guild_membership m
         JOIN guild g ON g.id = m.guild_id
         JOIN app_user u ON u.id = m.user_id
ORDER BY g.id, m.role, u.username;

\echo '== Refresh zetonai: DB laikoma tik SHA-256 maisa; atsauktas turi revoked_at'
SELECT r.id, u.username, left(r.token_hash, 16) || '...' AS token_hash, r.expires_at, r.revoked_at
FROM refresh_token r JOIN app_user u ON u.id = r.user_id
ORDER BY r.id DESC LIMIT 10;

\echo '== Irasai: autorius ir perziurejes DM (PENDING - be perziurejusio)'
SELECT l.id, q.guild_id, l.quest_id, l.status, a.username AS autorius, r.username AS perziurejo, l.xp_awarded
FROM log_entry l
         JOIN quest q ON q.id = l.quest_id
         JOIN app_user a ON a.id = l.author_id
         LEFT JOIN app_user r ON r.id = l.reviewer_id
ORDER BY l.id;

\echo '== DB apribojimai: vardas unikalus be raidziu dydzio, grupeje vienas DM, PENDING be perziurejusio'
SELECT indexname AS pavadinimas, indexdef AS apibrezimas FROM pg_indexes
WHERE indexname IN ('uq_app_user_username', 'uq_guild_one_dm')
UNION ALL
SELECT conname, pg_get_constraintdef(oid) FROM pg_constraint
WHERE conname IN ('uq_guild_membership', 'ck_log_pending_no_reviewer');
