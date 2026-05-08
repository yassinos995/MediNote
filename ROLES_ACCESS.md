# MediNote — Roles & Access Control

## Overview

Three roles exist. The role is set per user at creation and enforced on every API request via JWT.

| Role | Who | Data Access | User Management |
|------|-----|-------------|-----------------|
| `ADMIN` | System administrators | All 9 modules, all tables, all columns | Full CRUD on `/api/users` |
| `STAFF` | Internal staff | All 9 modules, all tables, all columns | None |
| `DELEGATE` | Field delegates | 7 modules only, restricted columns per table | None |

> `grm_users.password` and `grm_users.pass` are **always excluded** from query results for every role.

---

## ADMIN

Full unrestricted access to everything.

- All 9 modules
- All 129 tables
- All columns in every table
- Can create, update, enable/disable, and delete users via `/api/users/**`

---

## STAFF

Identical data access to ADMIN. Cannot manage users.

- All 9 modules
- All 129 tables
- All columns in every table
- No access to `/api/users/**`

---

## DELEGATE

Restricted to 7 of 9 modules. Within those modules, only specific columns per table are visible.

### Blocked modules (no access at all)

| Module | Tables |
|--------|--------|
| `FINANCE` | 21 tables (billing, budgets, cash flow, settlements, costs) |
| `ORGANISATION_TECHNIQUE` | 38 tables (HR, GPS, gifts, stock, user management) |

---

### Accessible modules & column restrictions

#### ANIMATION (15 tables)

| Table | Allowed Columns |
|-------|----------------|
| `annimation_challenge` | id, name, date_debut, date_fin, qte |
| `annimation_challenge_prod` | id, id_challenge, id_prod |
| `annimation_concurrent` | id, id_fiche, nom, type, value |
| `annimation_etat_formation` | id, id_fiche, id_prod, formation |
| `annimation_fiches` | id, id_pharmay, id_annimatrice, date_annimation, etat, personnel, points_fort, recommandations, finalisation, etatPharma, created_at, cancled_at |
| `annimation_fiches_grossiste` | id, id_fiche, id_grossiste |
| `annimation_fiches_personnel` | id, id_fiche, nom, prenom, gsm |
| `annimation_fiches_prescriptions` | id, id_fiche, id_prescripteur, potentiel, pour, produits_concurent |
| `annimation_fiches_produits` | id, id_prescription, id_prod |
| `annimation_formation` | id, date_debut, date_fin |
| `annimation_formation_products` | id, id_formation, id_prod |
| `annimation_potentiel` | id, valeur |
| `annimation_ventes` | id, id_fiche, id_prod, conseil, vente, formation |
| `annimation_verif_prescriptions` | id, id_fiche, id_prescripteur, commentaire, commentaire_annimatrice |
| `annimation_verif_produits` | id, id_prescription, id_prod |

#### VENTES (14 tables)

| Table | Allowed Columns |
|-------|----------------|
| `ca_gamme_real_time` | id, gamme, obj, rea, de, a |
| `ca_pdc_dlg` | id, id_del, caph, cagro, obj, year, prime |
| `ca_prd_day` | id, qte, ttc, ht, date, article |
| `ca_prd_month` | ttc, ht, qte, art, year, month |
| `ca_qte_prd_year_gro` | id, article, ca, ht, qte, year |
| `ca_tot_vente` | id, ttc, ht, qte, qte_g, art, cl, date, dlg, fam, zone |
| `ca_zone_real_time` | id, ca, ca_gro, zone, de, a |
| `ca_zone_year_fam` | ca, zone, fam, year |
| `cl_reliquat_dlg` | id, doc, date, dlg, ttc, rel, sens, cl, nomcl, zone, diff, fam, nomDlg |
| `cl_vente_prd_date` | id, cl, art, date, qte, ttc, ht |
| `fte_prd_day` | id, fte, year, article, de, a |
| `grat_prd_day` | id, grat, date, article |
| `data_concurant` | id, id_gro, de, a, id_prd, qte, t |
| `data_concurant_prd` | id, idprd, unitEch, unitnatreconts, unitReel, margeErr, canatreceonst, careelht, margeErrca, t, year |

#### PRODUITS_STOCK (6 tables)

| Table | Allowed Columns |
|-------|----------------|
| `art_entre_sortie` | id, qte, date, article, s |
| `art_prod_day` | id, article, qte, date |
| `art_stock_day` | id, article, qte, date |
| `art_vente_fam` | id, qte, grat, ttc, tht, date, fam, article |
| `art_vente_fam_export` | id, qte, grat, ttc, tht, date, fam, article |
| `art_zone` | id, article, zone, valeur |

#### DEMANDES (10 tables)

| Table | Allowed Columns |
|-------|----------------|
| `ba_demandes` | id, demande_id, type_pay, label, etat, commentaire |
| `ba_grouper` | id, ba_id, demande_id |
| `commentaire` | id, demande_id, com |
| `commentairecarte` | id, demande_id, com |
| `demande` | id, id_user, all_pros, prospects, secteur, delegation, Spec, type, date, lieu, dis, pays, budget_demander, budget_investi, objectif, labos, prod, commentaire, validation, public, validation_date, forHow |
| `demande_prod` | id, demande_id, prod_id, qte |
| `dis_demande` | dis |
| `detail_note_qual` | id, id_del, prime, note, de, a, par |
| `detail_note_qual_anim` | id, id_del, prime, note, de, a, par |
| `detail_note_qual_final` | id, id_del, com, note, de, a, par |

#### REFERENTIELS (12 tables)

| Table | Allowed Columns |
|-------|----------------|
| `activite` | id, nom |
| `africa` | id, name |
| `airet` | id, nom |
| `bu` | id, nom |
| `cible_val` | id, valeur |
| `d_type` | id, type |
| `date_jouv` | id, date_j |
| `departement` | id, nom |
| `gouv` | id, code, name |
| `gouvernerat` | id, nom, zone, poids, gouv |
| `groupe` | id, nom |
| `etablissement` | id, gouv_id, del_id, nom |

#### DOCUMENTS_ENQUETES (9 tables)

| Table | Allowed Columns |
|-------|----------------|
| `doc` | id, cat_id, titre, name, type, public |
| `doc_categorie` | id, nom |
| `ech_prd_day` | id, ech, date, article |
| `echant_demander` | id, par, pour, etat, date_validation, date_livraison |
| `echant_prod` | id, id_echant, id_prod, qte |
| `edit_prospect_dmd` | id, id_pros, nom, prenom, potentiel, spec, tel, gsm, email, adresse, service, code_postal, phyto, dermo, type_fid |
| `enquette_detail` | id, id_form, note, id_pros, date |
| `enquette_form` | id, nom, enquette, type |
| `etatph` | id, idph, carte, cmnt, prep |

#### MARKETING_PROMO (4 tables)

| Table | Allowed Columns |
|-------|----------------|
| `gratuite` | id, nbrProd, valGrat, etat |
| `grm_pb_type` | id, value, etat |
| `grm_promo_demander` | id, id_demande, id_cadeaux, qte |
| `grm_promotionnel` | id, id_demandeur, observation_client, date_remise_point, date_pro, etat, date_livraison |

---

## API Endpoint Access Summary

| Endpoint | ADMIN | STAFF | DELEGATE |
|----------|-------|-------|----------|
| `POST /api/auth/login` | ✅ | ✅ | ✅ |
| `POST /api/auth/refresh` | ✅ | ✅ | ✅ |
| `POST /api/auth/logout` | ✅ | ✅ | ✅ |
| `POST /api/auth/forgot-password` | ✅ | ✅ | ✅ |
| `GET /api/data/schema` | All modules | All modules | 7 modules, restricted columns |
| `GET /api/data/tables` | All 129 tables | All 129 tables | Allowed tables only |
| `GET /api/data/modules` | All 9 modules | All 9 modules | 7 modules |
| `GET /api/data/table/{table}` | All columns | All columns | Restricted columns |
| `POST /api/data/query/{table}` | All columns | All columns | Restricted columns |
| `POST /api/data/aggregate/{table}` | All columns | All columns | Restricted columns |
| `GET /api/users` | ✅ | ❌ 403 | ❌ 403 |
| `POST /api/users` | ✅ | ❌ 403 | ❌ 403 |
| `PUT /api/users/{id}` | ✅ | ❌ 403 | ❌ 403 |
| `DELETE /api/users/{id}` | ✅ | ❌ 403 | ❌ 403 |
