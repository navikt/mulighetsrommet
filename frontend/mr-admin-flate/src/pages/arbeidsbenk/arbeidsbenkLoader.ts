import {
  FeatureToggleService,
  NotificationsService,
  NotificationStatus,
  Tiltakskode,
  Toggles,
} from "@mr/api-client-v2";
import { QueryClient } from "@tanstack/react-query";
import { queryOptions } from "@tanstack/react-query";

const lesteNotifikasjonerQuery = queryOptions({
  queryKey: ["notifications", "leste"],
  queryFn: () =>
    NotificationsService.getNotifications({ query: { status: NotificationStatus.DONE } }),
});

const ulesteNotifikasjonerQuery = queryOptions({
  queryKey: ["notifications", "uleste"],
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
  const [{ data: leste }, { data: uleste }, { data: enableArbeidsbenk }] = await Promise.all([
    queryClient.ensureQueryData(lesteNotifikasjonerQuery),
    queryClient.ensureQueryData(ulesteNotifikasjonerQuery),
    queryClient.ensureQueryData(arbeidsbenkFeatureToggleQuery),
  ]);

  return {
    antallNotifikasjoner: leste?.pagination.totalCount + uleste?.pagination.totalCount,
    enableArbeidsbenk: enableArbeidsbenk,
  };
};
