create table users
(
    email         varchar(100) primary key,
    first_name    varchar(100),
    last_name     varchar(100),
    date_of_birth date
);

create table accounts
(
    phone_number       varchar(100) primary key,
    user_email         varchar(100) not null references users (email),
    pricing_plan_name  varchar(100),
    pricing_plan       integer,
    account_balance    bigint,
    last_take_off_date timestamp,
    is_active          boolean
);