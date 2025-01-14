import {
  FeatureToggleService,
  NotificationsService,
  NotificationStatus,
  Toggles,
} from "@mr/api-client";

export async function arbeidsbenkLoader() {
  const leste = await NotificationsService.getNotifications({ status: NotificationStatus.DONE });
  const uleste = await NotificationsService.getNotifications({
    status: NotificationStatus.NOT_DONE,
  });

  const enableArbeidsbenk = await FeatureToggleService.getFeatureToggle({
    feature: Toggles.MULIGHETSROMMET_ADMIN_FLATE_ARBEIDSBENK,
  });

  return {
    notifikasjoner: leste?.pagination.totalCount + uleste?.pagination.totalCount,
    enableArbeidsbenk: enableArbeidsbenk,
  };
}
