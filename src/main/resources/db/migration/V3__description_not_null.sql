-- Aprasymas niekada nebuna null: nenurodytas saugomas kaip tuscias tekstas.
-- Taip JSON atsakyme laukas "description" visada yra, o ne dingsta (null laukai i JSON neirasomi).

update guild set description = '' where description is null;
alter table guild alter column description set default '';
alter table guild alter column description set not null;

update quest set description = '' where description is null;
alter table quest alter column description set default '';
alter table quest alter column description set not null;
