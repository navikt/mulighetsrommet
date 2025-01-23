import { useMutation } from "@tanstack/react-query";
import { NotificationsService, SetNotificationStatusRequest } from "@mr/api-client-v2";

export function useMutateNotifikasjoner() {
  return useMutation({
    mutationKey: ["notifikasjoner"],
    mutationFn: (body: SetNotificationStatusRequest) => {
      return NotificationsService.setNotificationStatus({ body });
    },
  });
}
