# MediNote Backend

Spring Boot 3.x REST API (port **8081**) — the sole data layer for a multi-agent CRM chatbot. All agents route through this backend; none touch the database directly.

**Stack:** Java 17 · Spring Boot · MySQL (`vital` DB, 129 tables) · JWT auth (stateless) · BCrypt

**Roles:** `ADMIN` (full access + user management) · `STAFF` (full data access) · `DELEGATE` (7/9 modules, column-restricted)

---

## Endpoints

### Auth — `/api/auth`
| Method | Path | Auth required |
|--------|------|--------------|
| POST | `/api/auth/login` | No |
| POST | `/api/auth/refresh` | No |
| POST | `/api/auth/logout` | No |
| POST | `/api/auth/forgot-password` | No |
| POST | `/api/auth/reset-password` | No |

### Data — `/api/data`
| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/data/schema` | Full schema — all visible modules → tables → typed columns |
| GET | `/api/data/modules` | All modules and their table lists |
| GET | `/api/data/module/{name}/tables` | Tables in one module |
| GET | `/api/data/tables` | Flat list of all accessible tables |
| GET | `/api/data/table/{table}?page&size` | Paginated raw read |
| POST | `/api/data/query/{table}` | Filtered + sorted rows |
| POST | `/api/data/aggregate/{table}` | GROUP BY with SUM/COUNT/AVG/MIN/MAX |

### Metadata — `/api/meta`
| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/meta/columns/{table}` | Typed column list for one table |

### Users — `/api/users` *(ADMIN only)*
| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/users` | List all users |
| GET | `/api/users/{id}` | Get one user |
| POST | `/api/users` | Create user |
| PUT | `/api/users/{id}` | Update user |
| DELETE | `/api/users/{id}` | Delete user |

---

## Query body shapes

**`POST /api/data/query/{table}`**
```json
{
  "filters": [{ "column": "zone", "operator": "in", "value": ["ARIANA", "SFAX"] }],
  "sort":    { "column": "date", "direction": "desc" },
  "page": 0, "size": 50
}
```

**`POST /api/data/aggregate/{table}`**
```json
{
  "groupBy": ["zone"],
  "groupByExpressions": [{ "column": "date", "function": "YEAR", "alias": "yr" }],
  "metrics": [
    { "column": "ttc", "function": "SUM", "alias": "ca" },
    { "column": "*",   "function": "COUNT", "alias": "nb" }
  ],
  "filters": [{ "column": "date", "operator": "gte", "value": "2024-01-01" }],
  "having":  [{ "column": "ca", "operator": "gte", "value": 10000 }],
  "sorts":   [{ "column": "ca", "direction": "desc" }],
  "page": 0, "size": 20
}
```

**Filter operators:** `eq` `neq` `gt` `gte` `lt` `lte` `between` `in` `notIn` `like` `notLike` `startsWith` `endsWith` `contains` `isNull` `isNotNull`

**Aggregate functions:** `SUM` `COUNT` `AVG` `MIN` `MAX`

**GroupBy date functions:** `YEAR` `MONTH` `QUARTER` `WEEK` `DAY` `DATE`

---

## Unified response format

Every data endpoint returns the same shape:

```json
{
  "table": "ca_tot_vente",
  "page": 0, "size": 50,
  "totalRows": 1430, "totalPages": 29,
  "columns": ["dlg", "zone", "ttc"],
  "rows": [{ "dlg": "DLG01", "zone": "ARIANA", "ttc": 1250.0 }]
}
```

Max page size: **500 rows**.

---

## Security

- JWT required on all endpoints except `/api/auth/*`
- All SQL identifiers validated against `^[a-zA-Z0-9_]+$` before use
- All filter values bound as JDBC `?` parameters — never concatenated
- `grm_users.password` and `grm_users.pass` excluded from all responses for all roles
- DELEGATE blocked from FINANCE and ORGANISATION_TECHNIQUE modules entirely
