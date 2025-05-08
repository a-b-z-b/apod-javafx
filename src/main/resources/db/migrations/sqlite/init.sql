CREATE TABLE IF NOT EXISTS apod (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    title TEXT NOT NULL,
    date TEXT NOT NULL,
    explanation TEXT NOT NULL,
    media_type TEXT NOT NULL,
    copyright TEXT,
    hdurl TEXT,
    url TEXT
);