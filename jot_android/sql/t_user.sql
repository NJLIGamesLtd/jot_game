CREATE OR REPLACE TABLE t_user
(
	idUser INT NOT NULL AUTO_INCREMENT,
	vcUser VARCHAR(32) NOT NULL UNIQUE,
	vcMac VARCHAR(32) NOT NULL UNIQUE,
	bmSettings INT NOT NULL DEFAULT 0,
	nRanking SMALLINT NOT NULL DEFAULT 0,
	nGames SMALLINT NOT NULL DEFAULT 0,
	KEY (idUser),
	KEY (vcUser),
	KEY (vcMac)
);

INSERT INTO t_user VALUES
	( 0, 'Jot', 'internal', 0, 500, 1 ),
	( 1, 'JotRussell', 'mac', 0, 500, 1 );
