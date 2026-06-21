-- H2测试数据库初始化脚本
-- 注意：H2不支持MySQL的某些特性，需要调整语法

CREATE TABLE IF NOT EXISTS message_template (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(255) NOT NULL,
    platform_code VARCHAR(50) NOT NULL,
    sign_name VARCHAR(100),
    third_template_code VARCHAR(100),
    content TEXT,
    template_id VARCHAR(100),
    status TINYINT NOT NULL,
    creater BIGINT,
    updater BIGINT,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 插入测试数据
INSERT INTO message_template (name, platform_code, sign_name, third_template_code, content, template_id, status, creater, updater) 
VALUES ('验证码短信模板', 'ALIYUN', '睿课', 'SMS_123456789', '您的验证码是：{code}', 'TPL_001', 1, 1, 1);