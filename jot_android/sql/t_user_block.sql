CREATE OR REPLACE TABLE t_user_block
(
	idUserKey INT NOT NULL,
	idUserBlock INT NOT NULL,
	KEY (idUserKey),
	KEY (idUserKey, idUserBlock)
);
