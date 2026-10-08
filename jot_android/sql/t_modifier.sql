CREATE OR REPLACE TABLE t_modifier
(
	idModifier INT NOT NULL,
	vcModifier VARCHAR(32) NOT NULL,
	PRIMARY KEY (idModifier)
);

INSERT INTO t_modifier VALUES
	( 1, 'Invalid Word' ),
	( 2, 'Challenge Bonus' ),
	( 4, 'Challenge Lost' ),
	( 8, 'Exchange Tiles' ),
	( 16, 'Hint Provided' ),	
	( 32, 'Skip Turn' ),
	( 64, 'Quit Game' ),
	( 128, 'Game Over' );
