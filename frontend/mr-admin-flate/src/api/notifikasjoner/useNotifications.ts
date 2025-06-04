import {
  NotificationsService,
  NotificationStatus,
  SetNotificationStatusRequest,
} from "@mr/api-client-v2";
import { QueryKeys } from "@/api/QueryKeys";
import { useApiSuspenseQuery } from "@mr/frontend-common";
import { useMutation, useQueryClient } from "@tanstack/react-query";

export function useNotificationSummary() {
  return useApiSuspenseQuery({
    queryKey: QueryKeys.notificationsSummary(),
    queryFn: () => NotificationsService.getNotificationSummary(),
    refetchOnWindowFocus: true,
    refetchOnMount: true,
  });
}

export function useNotifications(status: NotificationStatus) {
  return useApiSuspenseQuery({
    queryKey: QueryKeys.notifications(status),
    queryFn: () => NotificationsService.getNotifications({ query: { status } }),
  });
}

export function useMutateNotifications() {
  const queryClient = useQueryClient();

  const mutation = useMutation({
    mutationKey: QueryKeys.notifications(),
    mutationFn: (body: SetNotificationStatusRequest) => {
      return NotificationsService.setNotificationStatus({ body });
    },
    onSuccess: async () => {
      await queryClient.invalidateQueries({
        queryKey: QueryKeys.notifications(),
      });
    },
  });

  function setNotificationStatus(notifikasjoner: { id: string; status: NotificationStatus }[]) {
    mutation.mutate({ notifikasjoner });
  }

  return { setNotificationStatus };
}
