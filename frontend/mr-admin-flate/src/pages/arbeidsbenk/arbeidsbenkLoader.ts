import {
  FeatureToggleService,
  NotificationsService,
  NotificationStatus,
  Tiltakskode,
  Toggles,
} from "@mr/api-client-v2";
import { queryOptions } from "@tanstack/react-query";
import { QueryKeys } from "../../api/QueryKeys";

export const ulesteNotifikasjonerQuery = queryOptions({
  queryKey: QueryKeys.notifikasjonerForAnsatt(NotificationStatus.NOT_DONE),
  queryFn: () =>
    NotificationsService.getNotifications({
      query: { status: NotificationStatus.NOT_DONE },
    }),
});

export const arbeidsbenkFeatureToggleQuery = queryOptions({
  queryKey: ["featureToggle", "arbeidsbenk"],
  queryFn: () =>
    FeatureToggleService.getFeatureToggle({
      query: {
        feature: Toggles.MULIGHETSROMMET_TILTAKSTYPE_MIGRERING_OKONOMI,
        tiltakskoder: [Tiltakskode.ARBEIDSFORBEREDENDE_TRENING],
      },
    }),
});
