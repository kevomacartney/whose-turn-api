CREATE KEYSPACE IF NOT EXISTS whose_turn
WITH REPLICATION = {
   'class' : 'SimpleStrategy',
   'replication_factor' : 1
};

CREATE TABLE whose_turn.$tableName (
    id UUID ,
    title TEXT,
    createdBy UUID,
    createdOn TIMESTAMP,
    lastUpdate TIMESTAMP,
    description TEXT,
    flagged BOOLEAN,
    category TEXT,
    priority TEXT,
    location TEXT,
    active BOOLEAN,
    PRIMARY KEY (id, createdBy, category)
)
