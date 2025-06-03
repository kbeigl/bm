-- ----------------------------------------------
-- create admin/admin user for Integration Tests
-- applied in mvn docker:build phase
SET @username = 'admin';
-- bcrypt hash for 'admin'
SET @password = '$2y$10$PswHScC9bcwK8IugkIQlp.oX.0CTIdugJTVJEyAysAX1AFmYDsV5S'; 
SET @email = 'admin@example.com';
-- 1 for admin, 0 for regular user
SET @administrator = 1; 

INSERT INTO users (name, hashedpassword, email, administrator, readonly, disabled, expirationtime, phone, map, latitude, longitude, zoom, twelvehourformat, attributes, userlimit, devicelimit, token)
VALUES (@username, @password, @email, @administrator, 0, 0, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL);


