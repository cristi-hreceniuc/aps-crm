create table profile (
  id bigint primary key auto_increment,
  user_id CHAR(36) not null,
  name varchar(120) not null,
  avatar_uri varchar(512),
  settings json null,
  created_at timestamp not null default current_timestamp,
  constraint fk_profile_user foreign key (user_id) references users(id)
);

create table module (
  id bigint primary key auto_increment,
  slug varchar(120) not null unique,
  title varchar(255) not null,
  intro_text text,
  position int not null,
  is_active bit not null default 1,
  is_premium bit not null default 0
);

create table submodule (
  id bigint primary key auto_increment,
  module_id bigint not null,
  slug varchar(120),
  title varchar(255) not null,
  intro_text text,
  position int not null,
  is_active bit not null default 1,
  constraint fk_submodule_module foreign key (module_id) references module(id)
);

create table lesson (
  id bigint primary key auto_increment,
  submodule_id bigint not null,
  code varchar(64),
  title varchar(255) not null,
  hint text,
  lesson_type varchar(40) not null,
  position int not null,
  is_active bit not null default 1,
  constraint fk_lesson_submodule foreign key (submodule_id) references submodule(id)
);

create table lesson_screen (
  id bigint primary key auto_increment,
  lesson_id bigint not null,
  screen_type varchar(40) not null,
  payload json not null,
  position int not null,
  constraint fk_screen_lesson foreign key (lesson_id) references lesson(id)
);

create table asset (
  id bigint primary key auto_increment,
  kind varchar(16) not null, -- IMAGE/AUDIO
  uri varchar(512) not null,
  mime varchar(100),
  checksum varchar(64),
  meta json null
);

create table profile_progress (
  id bigint primary key auto_increment,
  profile_id bigint not null,
  module_id bigint not null,
  submodule_id bigint not null,
  lesson_id bigint not null,
  screen_index int not null default 0,
  status varchar(20) not null, -- IN_PROGRESS, DONE
  updated_at timestamp not null default current_timestamp on update current_timestamp,
  constraint fk_pp_profile foreign key (profile_id) references profile(id)
);

create table profile_lesson_status (
  profile_id bigint not null,
  lesson_id bigint not null,
  status varchar(20) not null, -- LOCKED, UNLOCKED, DONE
  score int null,
  started_at timestamp null,
  finished_at timestamp null,
  primary key(profile_id, lesson_id),
  constraint fk_pls_profile foreign key (profile_id) references profile(id),
  constraint fk_pls_lesson foreign key (lesson_id) references lesson(id)
);
