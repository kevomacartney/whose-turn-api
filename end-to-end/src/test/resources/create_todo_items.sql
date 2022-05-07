CREATE TABLE todo_items_items_v1
(
    id UUID PRIMARY KEY,
    title TEXT NOT NULL,
    createdBy UUID NOT NULL,
    createdOn TIMESTAMP NOT NULL,
    lastUpdate TIMESTAMP NOT NULL,
    description TEXT,
    flagged BOOLEAN NOT NULL,
    category TEXT,
    priority TEXT NOT NULL,
    location TEXT,
    active BOOLEAN NOT NULL
);