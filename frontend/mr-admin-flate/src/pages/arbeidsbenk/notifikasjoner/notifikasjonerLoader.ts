import { NotificationsService, NotificationStatus } from "@mr/api-client-v2";
import { QueryClient } from "@tanstack/react-query";
import { queryOptions } from "@tanstack/react-query";
import { QueryKeys } from "../../../api/QueryKeys";

const lesteNotifikasjonerQuery = queryOptions({
  queryKey: QueryKeys.notifikasjonerForAnsatt(NotificationStatus.DONE),
  queryFn: async () =>
    await NotificationsService.getNotifications({ query: { status: NotificationStatus.DONE } }),
});

const ulesteNotifikasjonerQuery = queryOptions({
  queryKey: QueryKeys.notifikasjonerForAnsatt(NotificationStatus.NOT_DONE),
  queryFn: async () =>
    await NotificationsService.getNotifications({
      query: { status: NotificationStatus.NOT_DONE },
    }),
});

export const notifikasjonLoader = (queryClient: QueryClient) => async () => {
  const [leste, uleste] = await Promise.all([
    (await queryClient.ensureQueryData(lesteNotifikasjonerQuery)).data,
    (await queryClient.ensureQueryData(ulesteNotifikasjonerQuery)).data,
  ]);

  return { leste, uleste };
};
