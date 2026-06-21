-- 1. Masterclass Topics
INSERT INTO topic (id, name) VALUES (1, 'AI Strategy for Leaders');
INSERT INTO topic (id, name) VALUES (2, 'Managing Remote Engineering Teams');
INSERT INTO topic (id, name) VALUES (3, 'Financial Scaling for Startups');
INSERT INTO topic (id, name) VALUES (4, 'ABC...');
INSERT INTO topic (id, name) VALUES (5, 'EFG...');
INSERT INTO topic (id, name) VALUES (6, 'PQR...');
INSERT INTO topic (id, name) VALUES (7, 'LMN...');
INSERT INTO topic (id, name) VALUES (8, 'XYZ...');

-- 2. Speakers
INSERT INTO speaker (id, name) VALUES (1, 'Dr. Sarah Jenkins');
INSERT INTO speaker (id, name) VALUES (2, 'Prof. Alex Rivera');
INSERT INTO speaker (id, name) VALUES (3, 'Elena Rostova');

-- 3. Speaker-Topic roster (which speakers can deliver which topics)
INSERT INTO speaker_topic (speaker_id, topic_id) VALUES (1, 1);
INSERT INTO speaker_topic (speaker_id, topic_id) VALUES (1, 4);
INSERT INTO speaker_topic (speaker_id, topic_id) VALUES (2, 2);
INSERT INTO speaker_topic (speaker_id, topic_id) VALUES (2, 3);
INSERT INTO speaker_topic (speaker_id, topic_id) VALUES (3, 1);
INSERT INTO speaker_topic (speaker_id, topic_id) VALUES (3, 2);
INSERT INTO speaker_topic (speaker_id, topic_id) VALUES (3, 5);

INSERT INTO tenant_bucket (tenant_id, bucket_type, max_limit) VALUES ('vantage-fi', 'BUCKET_02', 3);
INSERT INTO tenant_bucket (tenant_id, bucket_type, max_limit) VALUES ('apex-edu', 'BUCKET_01', 1);
INSERT INTO tenant_bucket (tenant_id, bucket_type, max_limit) VALUES ('abc-edu', 'BUCKET_03', 8);

-- 4. Employees and cohort memberships

INSERT INTO employee (id, tenant_id, name, email_id, role)
VALUES ('emp-001', 'vantage-fi', 'Jane Doe', 'jane@vantage.com', 'EMPLOYEE');
INSERT INTO employee_cohorts (employee_id, cohort_id) VALUES ('emp-001', 'leadership-2026');
INSERT INTO employee_cohorts (employee_id, cohort_id) VALUES ('emp-001', 'managers-q2');

INSERT INTO employee (id, tenant_id, name, email_id, role)
VALUES ('emp-002', 'vantage-fi', 'Alex Chen', 'alex@vantage.com', 'EMPLOYEE');
INSERT INTO employee_cohorts (employee_id, cohort_id) VALUES ('emp-002', 'engineering');

-- 5. Seed Allocation Capacity Limits (Requirement #1)
-- Vantage Financial has allocated 2 total seats for the AI Strategy topic
INSERT INTO tenant_allocation (tenant_id, topic_id, allocated_slots) VALUES ('vantage-fi', 1, 2);
-- Apex Education has allocated 1 total seat for the Remote Teams topic
INSERT INTO tenant_allocation (tenant_id, topic_id, allocated_slots) VALUES ('apex-edu', 2, 1);

-- 6. Tenant cohort catalog (id + display name for CSV cohort_names matching)
INSERT INTO tenant_cohort (tenant_id, cohort_id, cohort_name) VALUES ('vantage-fi', 'leadership-2026', 'Senior Leadership');
INSERT INTO tenant_cohort (tenant_id, cohort_id, cohort_name) VALUES ('vantage-fi', 'ai-capability-build', 'AI Capability Build');
INSERT INTO tenant_cohort (tenant_id, cohort_id, cohort_name) VALUES ('vantage-fi', 'managers-q2', 'Managers Q2');
INSERT INTO tenant_cohort (tenant_id, cohort_id, cohort_name) VALUES ('vantage-fi', 'engineering', 'Engineering');