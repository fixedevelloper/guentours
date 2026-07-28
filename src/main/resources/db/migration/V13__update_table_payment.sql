-- 1. Passer temporairement en VARCHAR pour accepter n'importe quelle nouvelle valeur
ALTER TABLE payments MODIFY COLUMN payment_method VARCHAR(50) NULL;

-- 2. Nettoyer et rediriger les anciennes valeurs / NULL vers 'MOBILE_MONEY'
UPDATE payments
SET payment_method = 'MOBILE_MONEY'
WHERE payment_method NOT IN ('CARD', 'GOOGLE_PAY', 'APPLE_PAY', 'MOBILE_MONEY', 'PAYPAL')
   OR payment_method IS NULL;

-- 3. Appliquer la définition finale sous forme d'ENUM restreint
ALTER TABLE payments MODIFY COLUMN payment_method
    ENUM ('CARD', 'GOOGLE_PAY', 'APPLE_PAY', 'MOBILE_MONEY', 'PAYPAL') NOT NULL;