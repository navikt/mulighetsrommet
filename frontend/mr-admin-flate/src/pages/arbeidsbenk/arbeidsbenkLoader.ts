import {
  FeatureToggleService,
  NotificationsService,
  NotificationStatus,
  Tiltakskode,
  Toggles,
} from "@mr/api-client-v2";
import { QueryClient } from "@tanstack/react-query";
import { queryOptions } from "@tanstack/react-query";
import { QueryKeys } from "../../api/QueryKeys";

const ulesteNotifikasjonerQuery = queryOptions({
  queryKey: QueryKeys.notifikasjonerForAnsatt(NotificationStatus.NOT_DONE),
  queryFn: () =>
    NotificationsService.getNotifications({
      query: { status: NotificationStatus.NOT_DONE },
    }),
});

const arbeidsbenkFeatureToggleQuery = queryOptions({
  queryKey: ["featureToggle", "arbeidsbenk"],
  queryFn: () =>
    FeatureToggleService.getFeatureToggle({
      query: {
        feature: Toggles.MULIGHETSROMMET_TILTAKSTYPE_MIGRERING_OKONOMI,
        tiltakskoder: [Tiltakskode.ARBEIDSFORBEREDENDE_TRENING],
      },
    }),
});

export const arbeidsbenkLoader = (queryClient: QueryClient) => async () => {
  const [{ data: uleste }, { data: enableArbeidsbenk }] = await Promise.all([
    queryClient.ensureQueryData(ulesteNotifikasjonerQuery),
    queryClient.ensureQueryData(arbeidsbenkFeatureToggleQuery),
  ]);

  return {
    antallNotifikasjoner: uleste?.pagination.totalCount,
    enableArbeidsbenk: enableArbeidsbenk,
  };
};
