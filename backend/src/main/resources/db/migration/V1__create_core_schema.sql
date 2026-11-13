CREATE TABLE users (
    user_id UUID PRIMARY KEY,
    username VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(255),
    display_name VARCHAR(150) NOT NULL
);

CREATE TABLE projects (
    project_id UUID PRIMARY KEY,
    name VARCHAR(150) NOT NULL CHECK (char_length(name) BETWEEN 3 AND 150),
    description VARCHAR(2000) CHECK (description IS NULL OR char_length(description) <= 2000),
    owner_id UUID NOT NULL REFERENCES users(user_id) ON DELETE RESTRICT
);

CREATE TABLE tasks (
    task_id UUID PRIMARY KEY,
    project_id UUID NOT NULL REFERENCES projects(project_id) ON DELETE RESTRICT,
    creator_id UUID NOT NULL REFERENCES users(user_id) ON DELETE RESTRICT,
    title VARCHAR(150) NOT NULL CHECK (char_length(title) BETWEEN 3 AND 150),
    description VARCHAR(2000) CHECK (description IS NULL OR char_length(description) <= 2000),
    status VARCHAR(20) NOT NULL DEFAULT 'TODO' CHECK (status IN ('TODO', 'IN_PROGRESS', 'DONE')),
    priority VARCHAR(20) NOT NULL DEFAULT 'MEDIUM' CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH'))
);

CREATE TABLE task_assignments (
    task_id UUID NOT NULL REFERENCES tasks(task_id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(user_id) ON DELETE RESTRICT,
    CONSTRAINT pk_task_assignments PRIMARY KEY (task_id, user_id)
);

CREATE TABLE comments (
    comment_id UUID PRIMARY KEY,
    task_id UUID NOT NULL REFERENCES tasks(task_id) ON DELETE CASCADE,
    author_id UUID NOT NULL REFERENCES users(user_id) ON DELETE RESTRICT,
    content VARCHAR(2000) NOT NULL CHECK (
        char_length(content) BETWEEN 1 AND 2000 AND content ~ '[^[:space:]]'
    ),
    created_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_tasks_project_id ON tasks(project_id);
CREATE INDEX idx_task_assignments_user_id ON task_assignments(user_id);
CREATE INDEX idx_comments_task_id ON comments(task_id);
