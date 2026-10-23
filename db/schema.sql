-- =====================================================================
-- Order management database (JEE MVC workshop - FST Tanger, LSI)
-- Target: MySQL 8.0.16+ (CHECK constraints are enforced from this version)
-- Usage : mysql -u root -p < schema.sql
-- =====================================================================

DROP DATABASE IF EXISTS gestion_commandes;
CREATE DATABASE gestion_commandes
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE gestion_commandes;

-- ---------------------------------------------------------------------
-- CLIENT
-- ---------------------------------------------------------------------
CREATE TABLE client (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    nom         VARCHAR(80)  NOT NULL,
    prenom      VARCHAR(80)  NOT NULL,
    email       VARCHAR(150) NOT NULL,
    telephone   VARCHAR(20),
    adresse     VARCHAR(255),
    CONSTRAINT uq_client_email UNIQUE (email)
) ENGINE = InnoDB;

-- ---------------------------------------------------------------------
-- PRODUIT
-- ---------------------------------------------------------------------
CREATE TABLE produit (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    designation  VARCHAR(150)  NOT NULL,
    description  VARCHAR(500),
    prix         DECIMAL(10,2) NOT NULL,
    stock        INT           NOT NULL DEFAULT 0,
    CONSTRAINT ck_produit_prix  CHECK (prix >= 0),
    CONSTRAINT ck_produit_stock CHECK (stock >= 0)
) ENGINE = InnoDB;

-- ---------------------------------------------------------------------
-- COMMANDE (belongs to exactly one client)
-- A client cannot be deleted while he still has orders (RESTRICT).
-- ---------------------------------------------------------------------
CREATE TABLE commande (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    date_commande  DATE        NOT NULL DEFAULT (CURRENT_DATE),
    statut         VARCHAR(20) NOT NULL DEFAULT 'EN_ATTENTE',
    client_id      BIGINT      NOT NULL,
    CONSTRAINT fk_commande_client
        FOREIGN KEY (client_id) REFERENCES client (id)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT ck_commande_statut
        CHECK (statut IN ('EN_ATTENTE', 'VALIDEE', 'EXPEDIEE', 'ANNULEE'))
) ENGINE = InnoDB;

-- ---------------------------------------------------------------------
-- LIGNE_COMMANDE (composition: lines die with their order -> CASCADE)
-- prix_unitaire is a price snapshot taken when the line is created,
-- so later changes of produit.prix never rewrite order history.
-- ---------------------------------------------------------------------
CREATE TABLE ligne_commande (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    commande_id    BIGINT        NOT NULL,
    produit_id     BIGINT        NOT NULL,
    quantite       INT           NOT NULL,
    prix_unitaire  DECIMAL(10,2) NOT NULL,
    CONSTRAINT fk_ligne_commande
        FOREIGN KEY (commande_id) REFERENCES commande (id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_ligne_produit
        FOREIGN KEY (produit_id) REFERENCES produit (id)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT uq_ligne_commande_produit UNIQUE (commande_id, produit_id),
    CONSTRAINT ck_ligne_quantite CHECK (quantite > 0),
    CONSTRAINT ck_ligne_prix     CHECK (prix_unitaire >= 0)
) ENGINE = InnoDB;

CREATE INDEX idx_commande_client ON commande (client_id);
CREATE INDEX idx_ligne_produit   ON ligne_commande (produit_id);

-- ---------------------------------------------------------------------
-- Sample data (useful to test the CRUD screens quickly)
-- ---------------------------------------------------------------------
INSERT INTO client (nom, prenom, email, telephone, adresse) VALUES
    ('Alaoui', 'Yassine', 'yassine.alaoui@example.com', '0612345678', 'Tanger'),
    ('Bennani', 'Salma',  'salma.bennani@example.com',  '0698765432', 'Tetouan');

INSERT INTO produit (designation, description, prix, stock) VALUES
    ('Clavier mecanique', 'Clavier AZERTY retroeclaire', 450.00, 25),
    ('Souris sans fil',   'Souris ergonomique 2.4 GHz',  180.00, 40),
    ('Ecran 24 pouces',   'Dalle IPS Full HD',          1600.00, 10);

INSERT INTO commande (date_commande, statut, client_id) VALUES
    ('2026-10-20', 'VALIDEE', 1);

INSERT INTO ligne_commande (commande_id, produit_id, quantite, prix_unitaire) VALUES
    (1, 1, 1, 450.00),
    (1, 2, 2, 180.00);
