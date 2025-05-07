CREATE TABLE IF NOT EXISTS apod_image (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    title TEXT NOT NULL,
    date TEXT NOT NULL,
    explanation TEXT,
    copyright TEXT,
    hdurl TEXT
);

CREATE TABLE IF NOT EXISTS apod_video (
      id INTEGER PRIMARY KEY AUTOINCREMENT,
      title TEXT NOT NULL,
      date TEXT NOT NULL,
      explanation TEXT,
      url TEXT
);