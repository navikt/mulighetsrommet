import {
  FeatureToggleService,
  NotificationsService,
  NotificationStatus,
  Toggles,
} from "@mr/api-client-v2";

export async function arbeidsbenkLoader() {
  const [{ data: leste }, { data: uleste }, { data: enableArbeidsbenk }] = await Promise.all([
    NotificationsService.getNotifications({ query: { status: NotificationStatus.DONE } }),
    NotificationsService.getNotifications({
      query: { status: NotificationStatus.NOT_DONE },
    }),

    FeatureToggleService.getFeatureToggle({
      query: { feature: Toggles.MULIGHETSROMMET_ADMIN_FLATE_ARBEIDSBENK },
    }),
  ]);

  return {
    antallNotifikasjoner: leste?.pagination.totalCount + uleste?.pagination.totalCount,
    enableArbeidsbenk: enableArbeidsbenk,
  };
}
