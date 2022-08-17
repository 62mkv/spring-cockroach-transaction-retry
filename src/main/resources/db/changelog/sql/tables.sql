CREATE TABLE entity1 (
    id UUID primary key not null default uuid_generate_v4(),
    name varchar(50)
);

CREATE TABLE entity2 (
                         id UUID primary key not null default uuid_generate_v4(),
                         name varchar(50),
                         parent_id UUID not null
);