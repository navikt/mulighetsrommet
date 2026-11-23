output "view_ids" {
  value = {
    tiltakstype_view                                = module.mr_api_tiltakstype_view.bigquery_view_id
    avtale_view                                     = module.mr_api_avtale_view.bigquery_view_id
    gjennomforing_view                              = module.mr_api_gjennomforing_view.bigquery_view_id
    gjennomforing_nav_enhet_view                    = module.mr_api_gjennomforing_nav_enhet_view.bigquery_view_id
    avtale_nav_enhet_view                           = module.mr_api_avtale_nav_enhet_view.bigquery_view_id
    del_med_bruker_view                             = module.mr_api_del_med_bruker_view.bigquery_view_id
    utdanningsprogram_view                          = module.mr_api_utdanningsprogram_view.bigquery_view_id
    utdanning_view                                  = module.mr_api_utdanning_view.bigquery_view_id
    gjennomforing_utdanningsprogram_view            = module.mr_api_gjennomforing_utdanningsprogram_view.bigquery_view_id
    gjennomforing_enkeltplass_godkjenn_okonomi_view = module.mr_api_gjennomforing_enkeltplass_godkjenn_okonomi_view.bigquery_view_id
    tilsagn_enkeltplass_view                        = module.mr_api_tilsagn_enkeltplass_view.bigquery_view_id
    utbetaling_enkeltplass_view                     = module.mr_api_utbetaling_enkeltplass_view.bigquery_view_id
  }
}
