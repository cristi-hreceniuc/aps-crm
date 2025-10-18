create table if not exists refresh_token (
  id            bigint primary key auto_increment,
  user_id       CHAR(36) not null,
  token         varchar(255) not null unique,
  expires_at    timestamp not null,
  revoked       bit not null default 0,
  created_at    timestamp not null default current_timestamp,
  replaced_by   varchar(255) null,
  constraint fk_rt_user foreign key (user_id) references users(id)
) engine=InnoDB;