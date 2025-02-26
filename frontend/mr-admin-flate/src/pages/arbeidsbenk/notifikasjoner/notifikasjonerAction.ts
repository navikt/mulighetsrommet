import { NotificationsService, NotificationStatus } from "@mr/api-client-v2";
import { QueryClient } from "@tanstack/react-query";
import { ActionFunctionArgs } from "react-router";
import { QueryKeys } from "../../../api/QueryKeys";

export const setLestStatusForNotifikasjonAction =
  (queryClient: QueryClient) =>
  async ({ request }: ActionFunctionArgs) => {
    const formData = await request.formData();
    const id = String(formData.get("id"));
    const status = formData.get("status");

    if (!id) {
      throw Error("Id for notifikasjon ekisterer ikke");
    }

    if (status !== NotificationStatus.DONE && status !== NotificationStatus.NOT_DONE) {
      throw Error("Ugyldig status for notifikasjon");
    }

    await NotificationsService.setNotificationStatus({
      body: { notifikasjoner: [{ status, id }] },
    });

    await queryClient.invalidateQueries({
      queryKey: ["notifikasjoner"],
      type: "all",
    });

    await queryClient.invalidateQueries({
      queryKey: QueryKeys.antallUlesteNotifikasjoner(),
      type: "all",
    });
  };
