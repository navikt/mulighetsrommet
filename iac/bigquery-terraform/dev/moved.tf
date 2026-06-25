moved {
  from = google_bigquery_dataset.grafana_views
  to   = module.grafana_views.google_bigquery_dataset.grafana_views
}

moved {
  from = module.mr_datastream_vpc
  to   = module.mr_api_datastream_setup.module.mr_datastream_vpc
}

moved {
  from = module.mr_api_datastream
  to   = module.mr_api_datastream_setup.module.mr_api_datastream
}

moved {
  from = module.grafana_tilsagn_view
  to   = module.grafana_views.module.grafana_tilsagn_view
}

moved {
  from = module.grafana_utbetaling_view
  to   = module.grafana_views.module.grafana_utbetaling_view
}

moved {
  from = module.grafana_utbetaling_linje_view
  to   = module.grafana_views.module.grafana_utbetaling_linje_view
}

moved {
  from = module.grafana_avtale_view
  to   = module.grafana_views.module.grafana_avtale_view
}

moved {
  from = module.grafana_gjennomforing_view
  to   = module.grafana_views.module.grafana_gjennomforing_view
}

moved {
  from = module.mr_api_tiltakstype_view
  to   = module.datamarkedsplassen_views.module.mr_api_tiltakstype_view
}

moved {
  from = module.mr_api_avtale_view
  to   = module.datamarkedsplassen_views.module.mr_api_avtale_view
}

moved {
  from = module.mr_api_gjennomforing_view
  to   = module.datamarkedsplassen_views.module.mr_api_gjennomforing_view
}

moved {
  from = module.mr_api_gjennomforing_nav_enhet_view
  to   = module.datamarkedsplassen_views.module.mr_api_gjennomforing_nav_enhet_view
}

moved {
  from = module.mr_api_avtale_nav_enhet_view
  to   = module.datamarkedsplassen_views.module.mr_api_avtale_nav_enhet_view
}

moved {
  from = module.mr_api_del_med_bruker_view
  to   = module.datamarkedsplassen_views.module.mr_api_del_med_bruker_view
}

moved {
  from = module.mr_api_utdanningsprogram_view
  to   = module.datamarkedsplassen_views.module.mr_api_utdanningsprogram_view
}

moved {
  from = module.mr_api_utdanning_view
  to   = module.datamarkedsplassen_views.module.mr_api_utdanning_view
}

moved {
  from = module.mr_api_gjennomforing_utdanningsprogram_view
  to   = module.datamarkedsplassen_views.module.mr_api_gjennomforing_utdanningsprogram_view
}
