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

export const oppgaveoversiktFeatureToggleQuery = queryOptions({
  queryKey: ["featureToggle", "oppgaveoversikt"],
  queryFn: () =>
    FeatureToggleService.getFeatureToggle({
      query: {
        feature: Toggles.MULIGHETSROMMET_TILTAKSTYPE_MIGRERING_TILSAGN,
        tiltakskoder: [
          Tiltakskode.ARBEIDSFORBEREDENDE_TRENING,
          Tiltakskode.VARIG_TILRETTELAGT_ARBEID_SKJERMET,
        ],
      },
    }),
});
