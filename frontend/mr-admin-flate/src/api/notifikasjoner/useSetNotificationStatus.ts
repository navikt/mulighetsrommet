import { NotificationsService, NotificationStatus } from "@mr/api-client";
import { useMutation } from "@tanstack/react-query";
import { useNotificationSummary } from "./useNotificationSummary";

export interface SetNotificationStatusParams {
  status: NotificationStatus;
}

export function useSetNotificationStatus(id: string) {
  const { refetch: refetchNotificationSummary } = useNotificationSummary();

  return useMutation({
    mutationFn: async ({ status }: SetNotificationStatusParams): Promise<void> => {
      await NotificationsService.setNotificationStatus({
        requestBody: {
          notifikasjoner: [{ status, id }],
        },
      });

      await refetchNotificationSummary();
    },
  });
}
