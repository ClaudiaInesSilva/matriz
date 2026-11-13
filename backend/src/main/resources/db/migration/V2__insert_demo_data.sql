INSERT INTO users (user_id, username, password_hash, display_name) VALUES
    ('10000000-0000-4000-8000-000000000001', 'demo.alex', NULL, 'Alex Demo'),
    ('10000000-0000-4000-8000-000000000002', 'demo.sam', NULL, 'Sam Demo')
ON CONFLICT (user_id) DO NOTHING;

INSERT INTO projects (project_id, name, description, owner_id) VALUES
    ('20000000-0000-4000-8000-000000000001', 'Alex Demo Project', 'Seed project owned by Alex.', '10000000-0000-4000-8000-000000000001'),
    ('20000000-0000-4000-8000-000000000002', 'Sam Demo Project', 'Seed project owned by Sam.', '10000000-0000-4000-8000-000000000002')
ON CONFLICT (project_id) DO NOTHING;

INSERT INTO tasks (task_id, project_id, creator_id, title, description, status, priority) VALUES
    ('30000000-0000-4000-8000-000000000001', '20000000-0000-4000-8000-000000000001', '10000000-0000-4000-8000-000000000001', 'Review project requirements', 'Seed task for persistence verification.', 'TODO', 'MEDIUM'),
    ('30000000-0000-4000-8000-000000000002', '20000000-0000-4000-8000-000000000002', '10000000-0000-4000-8000-000000000002', 'Prepare project workspace', 'Seed task for persistence verification.', 'IN_PROGRESS', 'HIGH')
ON CONFLICT (task_id) DO NOTHING;

INSERT INTO task_assignments (task_id, user_id) VALUES
    ('30000000-0000-4000-8000-000000000001', '10000000-0000-4000-8000-000000000002'),
    ('30000000-0000-4000-8000-000000000002', '10000000-0000-4000-8000-000000000001')
ON CONFLICT (task_id, user_id) DO NOTHING;

INSERT INTO comments (comment_id, task_id, author_id, content, created_at) VALUES
    ('40000000-0000-4000-8000-000000000001', '30000000-0000-4000-8000-000000000001', '10000000-0000-4000-8000-000000000002', 'Seed comment for persistence verification.', TIMESTAMPTZ '2026-01-01 12:00:00+00')
ON CONFLICT (comment_id) DO NOTHING;
