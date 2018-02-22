CREATE TABLE t_counter (
              id bigint(11) NOT NULL,
              name varchar(45) NOT NULL,
              counter bigint(11) DEFAULT 0,
              PRIMARY KEY (id),
              UNIQUE KEY idx_t_counter_name (name)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8;

insert into t_counter(name, counter) values ('name1', 1);