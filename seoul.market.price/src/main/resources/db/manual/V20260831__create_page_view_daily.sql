CREATE TABLE page_view_daily (
    id BIGINT NOT NULL AUTO_INCREMENT,
    view_date DATE NOT NULL,
    view_count BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_page_view_daily_view_date UNIQUE (view_date)
);
