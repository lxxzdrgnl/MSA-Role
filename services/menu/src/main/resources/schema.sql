CREATE TABLE IF NOT EXISTS categories (
    id         INTEGER PRIMARY KEY AUTOINCREMENT,
    name       TEXT UNIQUE NOT NULL,
    sort_order INTEGER DEFAULT 0
);

CREATE TABLE IF NOT EXISTS menus (
    id                 INTEGER PRIMARY KEY AUTOINCREMENT,
    category_id        INTEGER NOT NULL REFERENCES categories(id),
    name               TEXT NOT NULL,
    description        TEXT,
    price              INTEGER NOT NULL,
    image_url          TEXT,
    tags               TEXT,
    allergens          TEXT,
    spicy_level        INTEGER DEFAULT 0,
    cook_time_minutes  INTEGER DEFAULT 15,
    is_sold_out        INTEGER DEFAULT 0,
    is_best            INTEGER DEFAULT 0,
    created_at         DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at         DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- Seed categories
INSERT OR IGNORE INTO categories (id, name, sort_order) VALUES (1, '한식', 1);
INSERT OR IGNORE INTO categories (id, name, sort_order) VALUES (2, '중식', 2);
INSERT OR IGNORE INTO categories (id, name, sort_order) VALUES (3, '일식', 3);
INSERT OR IGNORE INTO categories (id, name, sort_order) VALUES (4, '분식', 4);
INSERT OR IGNORE INTO categories (id, name, sort_order) VALUES (5, '음료', 5);

-- Seed menus
INSERT OR IGNORE INTO menus (id, category_id, name, description, price, tags, allergens, spicy_level, cook_time_minutes)
VALUES (1, 1, '김치찌개', '매콤한 돼지고기 김치찌개. 깊은 국물 맛이 일품입니다.', 8000, '매운,고기,국물', '대두', 2, 20);

INSERT OR IGNORE INTO menus (id, category_id, name, description, price, tags, allergens, spicy_level, cook_time_minutes)
VALUES (2, 1, '된장찌개', '구수한 된장에 두부와 야채를 넣어 끓인 전통 찌개.', 7500, '국물,전통,건강', '대두', 0, 20);

INSERT OR IGNORE INTO menus (id, category_id, name, description, price, tags, allergens, spicy_level, cook_time_minutes)
VALUES (3, 1, '불고기', '달콤한 양념에 재운 소고기 불고기. 부드럽고 감칠맛 가득.', 12000, '고기,달콤,구이', '대두,밀', 0, 25);

INSERT OR IGNORE INTO menus (id, category_id, name, description, price, tags, allergens, spicy_level, cook_time_minutes)
VALUES (4, 1, '제육볶음', '매콤달콤한 고추장 양념의 돼지고기 볶음.', 9000, '매운,고기,볶음', '대두,밀', 2, 15);

INSERT OR IGNORE INTO menus (id, category_id, name, description, price, tags, allergens, spicy_level, cook_time_minutes)
VALUES (5, 2, '짜장면', '춘장으로 볶은 진한 소스의 짜장면. 양파와 고기가 듬뿍.', 7000, '면,중화,달콤', '밀,대두', 0, 15);

INSERT OR IGNORE INTO menus (id, category_id, name, description, price, tags, allergens, spicy_level, cook_time_minutes)
VALUES (6, 2, '짬뽕', '해산물과 야채가 가득한 매콤한 국물 짬뽕.', 8000, '면,매운,해산물', '밀,갑각류,대두', 3, 15);

INSERT OR IGNORE INTO menus (id, category_id, name, description, price, tags, allergens, spicy_level, cook_time_minutes)
VALUES (7, 3, '연어 초밥', '신선한 연어를 올린 초밥 세트 (8피스).', 15000, '초밥,생선,신선', '생선', 0, 10);

INSERT OR IGNORE INTO menus (id, category_id, name, description, price, tags, allergens, spicy_level, cook_time_minutes)
VALUES (8, 3, '돈카츠', '바삭한 튀김옷의 두툼한 등심 돈카츠. 특제 소스 포함.', 11000, '튀김,고기,바삭', '밀,대두,달걀', 0, 20);

INSERT OR IGNORE INTO menus (id, category_id, name, description, price, tags, allergens, spicy_level, cook_time_minutes)
VALUES (9, 4, '떡볶이', '고추장 양념의 쫄깃한 떡볶이. 어묵과 삶은 달걀 포함.', 5000, '매운,떡,간식', '밀,대두,달걀', 2, 10);

INSERT OR IGNORE INTO menus (id, category_id, name, description, price, tags, allergens, spicy_level, cook_time_minutes)
VALUES (10, 4, '김밥', '야채와 단무지, 햄이 들어간 클래식 김밥.', 3500, '간식,밥,간편', '밀,대두,달걀', 0, 10);

INSERT OR IGNORE INTO menus (id, category_id, name, description, price, tags, allergens, spicy_level, cook_time_minutes)
VALUES (11, 5, '아메리카노', '깊고 진한 에스프레소 원두로 내린 아메리카노.', 3000, '커피,음료,카페인', '', 0, 3);

INSERT OR IGNORE INTO menus (id, category_id, name, description, price, tags, allergens, spicy_level, cook_time_minutes)
VALUES (12, 5, '녹차 라떼', '고소한 우유와 향긋한 녹차가 조화로운 라떼.', 4500, '음료,녹차,부드러운', '우유', 0, 5);
