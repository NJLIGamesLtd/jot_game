CREATE OR REPLACE TABLE t_setting
(
	idSetting INT NOT NULL,
	vcSetting VARCHAR(32) NOT NULL,
	PRIMARY KEY (idSetting)
);

INSERT INTO t_setting VALUES
	( 1, 'Invalid Word Warning' ),
	( 2, 'Allow One-Time Hint' ),
	( 4, '' );
