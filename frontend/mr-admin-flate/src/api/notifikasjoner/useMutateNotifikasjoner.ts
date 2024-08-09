import { useMutation } from "@tanstack/react-query";
import {
  ApiError,
  NotificationsService,
  SetNotificationStatusRequest,
  SetNotificationStatusResponse,
} from "mulighetsrommet-api-client";

export function useMutateNotifikasjoner() {
  return useMutation<SetNotificationStatusResponse, ApiError, SetNotificationStatusRequest>({
    mutationKey: ["notifikasjoner"],
    mutationFn: (requestBody) => {
      return NotificationsService.setNotificationStatus({ requestBody });
    },
  });
}
