package com.medinote.medinotebackend.dataagent.service;

import com.medinote.medinotebackend.dataagent.model.DataModule;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class ModuleRegistryService {

    // Map.ofEntries used (Map.of is capped at 10 keys)
    private static final Map<DataModule, List<String>> MODULE_TABLES = Map.ofEntries(

            Map.entry(DataModule.ANIMATION, List.of(
                    "annimation_challenge",
                    "annimation_challenge_prod",
                    "annimation_concurrent",
                    "annimation_etat_formation",
                    "annimation_fiches",
                    "annimation_fiches_grossiste",
                    "annimation_fiches_personnel",
                    "annimation_fiches_prescriptions",
                    "annimation_fiches_produits",
                    "annimation_formation",
                    "annimation_formation_products",
                    "annimation_potentiel",
                    "annimation_ventes",
                    "annimation_verif_prescriptions",
                    "annimation_verif_produits"
            )),

            Map.entry(DataModule.VENTES, List.of(
                    "ca_gamme_real_time",
                    "ca_pdc_dlg",
                    "ca_prd_day",
                    "ca_prd_month",
                    "ca_qte_prd_year_gro",
                    "ca_tot_vente",
                    "ca_zone_real_time",
                    "ca_zone_year_fam",
                    "cl_reliquat_dlg",
                    "cl_vente_prd_date",
                    "fte_prd_day",
                    "grat_prd_day",
                    "data_concurant",
                    "data_concurant_prd"
            )),

            Map.entry(DataModule.PRODUITS_STOCK, List.of(
                    "art_entre_sortie",
                    "art_prod_day",
                    "art_stock_day",
                    "art_vente_fam",
                    "art_vente_fam_export",
                    "art_zone"
            )),

            Map.entry(DataModule.DEMANDES, List.of(
                    "ba_demandes",
                    "ba_grouper",
                    "commentaire",
                    "commentairecarte",
                    "demande",
                    "demande_prod",
                    "dis_demande",
                    "detail_note_qual",
                    "detail_note_qual_anim",
                    "detail_note_qual_final"
            )),

            Map.entry(DataModule.REFERENTIELS, List.of(
                    "activite",
                    "africa",
                    "airet",
                    "bu",
                    "cible_val",
                    "d_type",
                    "date_jouv",
                    "departement",
                    "gouv",
                    "gouvernerat",
                    "groupe",
                    "etablissement"
            )),

            Map.entry(DataModule.DOCUMENTS_ENQUETES, List.of(
                    "doc",
                    "doc_categorie",
                    "ech_prd_day",
                    "echant_demander",
                    "echant_prod",
                    "edit_prospect_dmd",
                    "enquette_detail",
                    "enquette_form",
                    "etatph"
            )),

            Map.entry(DataModule.MARKETING_PROMO, List.of(
                    "gratuite",
                    "grm_pb_type",
                    "grm_promo_demander",
                    "grm_promotionnel"
            )),

            Map.entry(DataModule.FINANCE, List.of(
                    "art_cogs",
                    "bl_fact",
                    "budget_conso",
                    "budget_prd_day",
                    "ca_gro_day",
                    "ca_pdc_r_real_time",
                    "ca_prd_gro_zone",
                    "ca_prd_ph_zone",
                    "ca_real_time",
                    "cl_encours",
                    "cl_fr_reliquat",
                    "cl_reglement",
                    "cl_reglement_all",
                    "cl_reglement_d",
                    "cl_reglement_enc",
                    "cl_reliquat",
                    "cl_vente_ech",
                    "cout",
                    "fournisseur",
                    "frais_forf",
                    "frn_reglement"
            )),

            Map.entry(DataModule.ORGANISATION_TECHNIQUE, List.of(
                    "affectation",
                    "affectation2",
                    "affectation_uniges_ph",
                    "auto_affectation",
                    "auto_liste",
                    "autre_note_frais",
                    "bs",
                    "cible",
                    "conf_moyenne_visites",
                    "dcrepcode",
                    "deleg_dlg",
                    "delegation",
                    "delres",
                    "detail_note_qual_cdp",
                    "detail_note_qual_hb",
                    "detail_note_qual_kam",
                    "detail_note_qual_sup",
                    "doublons_prime_superviseur",
                    "doublons_rh_note_superviseur",
                    "gps_stops",
                    "gpsdel",
                    "grm_art_version",
                    "grm_budget_annuel",
                    "grm_budget_annuel_zone",
                    "grm_cadeaux_demander",
                    "grm_demande_cadeaux",
                    "grm_fournisseur",
                    "grm_gift",
                    "grm_gift_family",
                    "grm_gift_save",
                    "grm_gift_save_b",
                    "grm_gift_save_sup_prod",
                    "grm_stock",
                    "grm_uniges",
                    "grm_user_type",
                    "grm_users",
                    "his_affectation",
                    "hisdelres"
            ))
    );

    public List<String> getTablesForModule(DataModule module) {
        return MODULE_TABLES.getOrDefault(module, List.of());
    }

    public Set<DataModule> getAllModules() {
        return MODULE_TABLES.keySet();
    }

    public DataModule findModuleByTable(String tableName) {
        String normalized = tableName.toLowerCase();
        return MODULE_TABLES.entrySet().stream()
                .filter(entry -> entry.getValue().contains(normalized))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(null);
    }
}
