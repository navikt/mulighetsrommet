import { NotificationsService, NotificationStatus } from "@mr/api-client-v2";
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

export const notifikasjonLoader = (queryClient: QueryClient) => async () => {
  const [{ data: leste }, { data: uleste }] = await Promise.all([
    queryClient.ensureQueryData(lesteNotifikasjonerQuery),
    queryClient.ensureQueryData(ulesteNotifikasjonerQuery),
  ]);

  return { leste, uleste };
};
