-- -----------------------------------------------------
-- PostgreSQL Schema for Socket.IO Chat Application
-- -----------------------------------------------------

-- Table structure for files
DROP TABLE IF EXISTS files CASCADE;
CREATE TABLE files (
  FileID SERIAL PRIMARY KEY,
  FileExtension VARCHAR(255) DEFAULT NULL,
  BlurHash VARCHAR(255) DEFAULT NULL,
  Status CHAR(1) NOT NULL DEFAULT '0'
);

-- Table structure for user
DROP TABLE IF EXISTS "user" CASCADE;
CREATE TABLE "user" (
  UserID SERIAL PRIMARY KEY,
  UserName VARCHAR(255) UNIQUE DEFAULT NULL,
  Password VARCHAR(72) NOT NULL
);

-- Table structure for user_account
DROP TABLE IF EXISTS user_account CASCADE;
CREATE TABLE user_account (
  UserID INT PRIMARY KEY,
  UserName VARCHAR(255) DEFAULT NULL,
  Gender CHAR(1) NOT NULL DEFAULT '',
  Image BYTEA,
  ImageString VARCHAR(255) DEFAULT '',
  Status CHAR(1) NOT NULL DEFAULT '1',
  CONSTRAINT fk_user_account_user FOREIGN KEY (UserID) REFERENCES "user" (UserID) ON DELETE CASCADE ON UPDATE CASCADE
);

-- Indexes for performance
CREATE INDEX idx_user_username ON "user"(UserName);
CREATE INDEX idx_user_account_status ON user_account(Status);

-- Reset serial sequence to start after pre-inserted IDs
SELECT setval('user_userid_seq', 100);
