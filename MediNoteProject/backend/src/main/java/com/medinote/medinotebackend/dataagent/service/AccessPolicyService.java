package com.medinote.medinotebackend.dataagent.service;

import com.medinote.medinotebackend.dataagent.model.DataModule;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AccessPolicyService {

    // ── Roles (must match users.role enum: ADMIN, STAFF, DELEGATE) ───────────
    private static final String ROLE_ADMIN    = "ADMIN";
    private static final String ROLE_STAFF    = "STAFF";
    private static final String ROLE_DELEGATE = "DELEGATE";

    // ── Sensitive columns excluded for ALL roles ──────────────────────────────
    private static final Map<String, List<String>> SENSITIVE_COLUMNS = Map.of(
            "grm_users", List.of("password", "pass")
    );

    // ── Modules the DELEGATE role can access ─────────────────────────────────
    private static final Set<DataModule> DELEGATE_ALLOWED_MODULES = Set.of(
            DataModule.ANIMATION,
            DataModule.VENTES,
            DataModule.PRODUITS_STOCK,
            DataModule.DEMANDES,
            DataModule.REFERENTIELS,
            DataModule.DOCUMENTS_ENQUETES,
            DataModule.MARKETING_PROMO
    );

    // ── Column-level access for DELEGATE ─────────────────────────────────────
    private static final Map<String, List<String>> DELEGATE_ALLOWED_COLUMNS = Map.ofEntries(

            Map.entry("activite",                       List.of("id", "nom")),
            Map.entry("africa",                         List.of("id", "name")),
            Map.entry("airet",                          List.of("id", "nom")),
            Map.entry("annimation_challenge",           List.of("id", "name", "date_debut", "date_fin", "qte")),
            Map.entry("annimation_challenge_prod",      List.of("id", "id_challenge", "id_prod")),
            Map.entry("annimation_concurrent",          List.of("id", "id_fiche", "nom", "type", "value")),
            Map.entry("annimation_etat_formation",      List.of("id", "id_fiche", "id_prod", "formation")),
            Map.entry("annimation_fiches",              List.of("id", "id_pharmay", "id_annimatrice",
                    "date_annimation", "etat", "personnel", "points_fort", "recommandations",
                    "finalisation", "etatPharma", "created_at", "cancled_at")),
            Map.entry("annimation_fiches_grossiste",    List.of("id", "id_fiche", "id_grossiste")),
            Map.entry("annimation_fiches_personnel",    List.of("id", "id_fiche", "nom", "prenom", "gsm")),
            Map.entry("annimation_fiches_prescriptions",List.of("id", "id_fiche", "id_prescripteur",
                    "potentiel", "pour", "produits_concurent")),
            Map.entry("annimation_fiches_produits",     List.of("id", "id_prescription", "id_prod")),
            Map.entry("annimation_formation",           List.of("id", "date_debut", "date_fin")),
            Map.entry("annimation_formation_products",  List.of("id", "id_formation", "id_prod")),
            Map.entry("annimation_potentiel",           List.of("id", "valeur")),
            Map.entry("annimation_ventes",              List.of("id", "id_fiche", "id_prod", "conseil", "vente", "formation")),
            Map.entry("annimation_verif_prescriptions", List.of("id", "id_fiche", "id_prescripteur",
                    "commentaire", "commentaire_annimatrice")),
            Map.entry("annimation_verif_produits",      List.of("id", "id_prescription", "id_prod")),
            Map.entry("art_entre_sortie",               List.of("id", "qte", "date", "article", "s")),
            Map.entry("art_prod_day",                   List.of("id", "article", "qte", "date")),
            Map.entry("art_stock_day",                  List.of("id", "article", "qte", "date")),
            Map.entry("art_vente_fam",                  List.of("id", "qte", "grat", "ttc", "tht", "date", "fam", "article")),
            Map.entry("art_vente_fam_export",           List.of("id", "qte", "grat", "ttc", "tht", "date", "fam", "article")),
            Map.entry("art_zone",                       List.of("id", "article", "zone", "valeur")),
            Map.entry("ba_demandes",                    List.of("id", "demande_id", "type_pay", "label", "etat", "commentaire")),
            Map.entry("ba_grouper",                     List.of("id", "ba_id", "demande_id")),
            Map.entry("bu",                             List.of("id", "nom")),
            Map.entry("ca_gamme_real_time",             List.of("id", "gamme", "obj", "rea", "de", "a")),
            Map.entry("ca_pdc_dlg",                     List.of("id", "id_del", "caph", "cagro", "obj", "year", "prime")),
            Map.entry("ca_prd_day",                     List.of("id", "qte", "ttc", "ht", "date", "article")),
            Map.entry("ca_prd_month",                   List.of("ttc", "ht", "qte", "art", "year", "month")),
            Map.entry("ca_qte_prd_year_gro",            List.of("id", "article", "ca", "ht", "qte", "year")),
            Map.entry("ca_tot_vente",                   List.of("id", "ttc", "ht", "qte", "qte_g", "art", "cl", "date", "dlg", "fam", "zone")),
            Map.entry("ca_zone_real_time",              List.of("id", "ca", "ca_gro", "zone", "de", "a")),
            Map.entry("ca_zone_year_fam",               List.of("ca", "zone", "fam", "year")),
            Map.entry("cible_val",                      List.of("id", "valeur")),
            Map.entry("cl_reliquat_dlg",                List.of("id", "doc", "date", "dlg", "ttc", "rel",
                    "sens", "cl", "nomcl", "zone", "diff", "fam", "nomDlg")),
            Map.entry("cl_vente_prd_date",              List.of("id", "cl", "art", "date", "qte", "ttc", "ht")),
            Map.entry("commentaire",                    List.of("id", "demande_id", "com")),
            Map.entry("commentairecarte",               List.of("id", "demande_id", "com")),
            Map.entry("d_type",                         List.of("id", "type")),
            Map.entry("data_concurant",                 List.of("id", "id_gro", "de", "a", "id_prd", "qte", "t")),
            // fixed: unitnatreceonst -> unitnatreconts (matches actual DB column name)
            Map.entry("data_concurant_prd",             List.of("id", "idprd", "unitEch", "unitnatreconts",
                    "unitReel", "margeErr", "canatreceonst", "careelht", "margeErrca", "t", "year")),
            Map.entry("date_jouv",                      List.of("id", "date_j")),
            Map.entry("demande",                        List.of("id", "id_user", "all_pros", "prospects",
                    "secteur", "delegation", "Spec", "type", "date", "lieu", "dis", "pays",
                    "budget_demander", "budget_investi", "objectif", "labos", "prod",
                    "commentaire", "validation", "public", "validation_date", "forHow")),
            // fixed: id_prod -> prod_id (matches actual DB column name)
            Map.entry("demande_prod",                   List.of("id", "demande_id", "prod_id", "qte")),
            Map.entry("departement",                    List.of("id", "nom")),
            Map.entry("detail_note_qual",               List.of("id", "id_del", "prime", "note", "de", "a", "par")),
            Map.entry("detail_note_qual_anim",          List.of("id", "id_del", "prime", "note", "de", "a", "par")),
            Map.entry("detail_note_qual_final",         List.of("id", "id_del", "com", "note", "de", "a", "par")),
            Map.entry("dis_demande",                    List.of("dis")),
            Map.entry("doc",                            List.of("id", "cat_id", "titre", "name", "type", "public")),
            Map.entry("doc_categorie",                  List.of("id", "nom")),
            Map.entry("ech_prd_day",                    List.of("id", "ech", "date", "article")),
            Map.entry("echant_demander",                List.of("id", "par", "pour", "etat", "date_validation", "date_livraison")),
            Map.entry("echant_prod",                    List.of("id", "id_echant", "id_prod", "qte")),
            Map.entry("edit_prospect_dmd",              List.of("id", "id_pros", "nom", "prenom", "potentiel",
                    "spec", "tel", "gsm", "email", "adresse", "service", "code_postal",
                    "phyto", "dermo", "type_fid")),
            Map.entry("enquette_detail",                List.of("id", "id_form", "note", "id_pros", "date")),
            Map.entry("enquette_form",                  List.of("id", "nom", "enquette", "type")),
            Map.entry("etablissement",                  List.of("id", "gouv_id", "del_id", "nom")),
            Map.entry("etatph",                         List.of("id", "idph", "carte", "cmnt", "prep")),
            Map.entry("fte_prd_day",                    List.of("id", "fte", "year", "article", "de", "a")),
            Map.entry("gouv",                           List.of("id", "code", "name")),
            Map.entry("gouvernerat",                    List.of("id", "nom", "zone", "poids", "gouv")),
            Map.entry("grat_prd_day",                   List.of("id", "grat", "date", "article")),
            Map.entry("gratuite",                       List.of("id", "nbrProd", "valGrat", "etat")),
            Map.entry("grm_pb_type",                    List.of("id", "value", "etat")),
            Map.entry("grm_promo_demander",             List.of("id", "id_demande", "id_cadeaux", "qte")),
            Map.entry("grm_promotionnel",               List.of("id", "id_demandeur", "observation_client",
                    "date_remise_point", "date_pro", "etat", "date_livraison")),
            Map.entry("groupe",                         List.of("id", "nom"))
    );

    // ─────────────────────────────────────────────────────────────────────────

    private final ModuleRegistryService moduleRegistryService;

    public AccessPolicyService(ModuleRegistryService moduleRegistryService) {
        this.moduleRegistryService = moduleRegistryService;
    }

    public boolean hasFullAccess(String role) {
        return ROLE_ADMIN.equalsIgnoreCase(role)
                || ROLE_STAFF.equalsIgnoreCase(role);
    }

    public boolean canAccessModule(String role, DataModule module) {
        if (hasFullAccess(role)) return true;
        if (ROLE_DELEGATE.equalsIgnoreCase(role)) return DELEGATE_ALLOWED_MODULES.contains(module);
        return false;
    }

    public Set<DataModule> getAllowedModules(String role) {
        if (hasFullAccess(role)) return moduleRegistryService.getAllModules();
        if (ROLE_DELEGATE.equalsIgnoreCase(role)) return DELEGATE_ALLOWED_MODULES;
        return Set.of();
    }

    public boolean canReadTable(String role, String tableName) {
        String normalized = tableName.toLowerCase();
        if (hasFullAccess(role)) return true;
        if (ROLE_DELEGATE.equalsIgnoreCase(role)) {
            DataModule module = moduleRegistryService.findModuleByTable(normalized);
            return module != null
                    && DELEGATE_ALLOWED_MODULES.contains(module)
                    && DELEGATE_ALLOWED_COLUMNS.containsKey(normalized);
        }
        return false;
    }

    public List<String> getAllowedColumns(String role, String tableName) {
        String normalized = tableName.toLowerCase();
        if (hasFullAccess(role)) return List.of("*");
        if (ROLE_DELEGATE.equalsIgnoreCase(role)) {
            return DELEGATE_ALLOWED_COLUMNS.getOrDefault(normalized, List.of());
        }
        return List.of();
    }

    /** Columns that must never be returned regardless of role. */
    public List<String> getExcludedColumns(String tableName) {
        return SENSITIVE_COLUMNS.getOrDefault(tableName.toLowerCase(), List.of());
    }

    public List<String> getAllowedTablesForRole(String role) {
        if (hasFullAccess(role)) {
            return moduleRegistryService.getAllModules().stream()
                    .flatMap(m -> moduleRegistryService.getTablesForModule(m).stream())
                    .sorted()
                    .toList();
        }
        if (ROLE_DELEGATE.equalsIgnoreCase(role)) {
            return DELEGATE_ALLOWED_MODULES.stream()
                    .flatMap(m -> moduleRegistryService.getTablesForModule(m).stream())
                    .filter(DELEGATE_ALLOWED_COLUMNS::containsKey)
                    .sorted()
                    .toList();
        }
        return List.of();
    }

    public Map<DataModule, List<String>> getAllowedModulesWithTables(String role) {
        return getAllowedModules(role).stream()
                .sorted()
                .collect(Collectors.toMap(
                        module -> module,
                        module -> moduleRegistryService.getTablesForModule(module).stream()
                                .filter(table -> canReadTable(role, table))
                                .sorted()
                                .toList()
                ));
    }
}
