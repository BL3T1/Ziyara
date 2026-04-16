-- Spring Modulith JPA event publication table (minimal schema for tests/dev).
create table if not exists event_publication (
    id uuid primary key,
    publication_date timestamp not null,
    completion_date timestamp null,
    last_resubmission_date timestamp null,
    completion_attempts integer not null default 0,
    listener_id varchar(255) not null,
    event_type varchar(500) not null,
    serialized_event text not null,
    status varchar(50) not null
);

create index if not exists idx_event_publication_completion_date on event_publication (completion_date);
create index if not exists idx_event_publication_publication_date on event_publication (publication_date);
