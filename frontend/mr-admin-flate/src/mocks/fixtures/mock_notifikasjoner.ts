import {PaginertUserNotifications, UserNotification} from "mulighetsrommet-api-client";

export const mockNotifikasjoner: PaginertUserNotifications = {  pagination: {
    totalCount: 2,
    currentPage: 1,
    pageSize: 50,
  },
  data: [
  {
    id: "d65c2be1-980d-4782-a51f-0ad9b52c3fda",
    type: UserNotification.type.NOTIFICATION,
    title: "Avtalen Oppfølging, tjenesteområde F - Moss utløper om 30.06.2023",
    description: "Beskrivelse",
    user: "B99876",
    createdAt: "2023-01-26T13:51:50.417-07:00",
    readAt: "2023-01-26T13:51:50.417-07:00",
  },
  {
    id: "eccd9a93-3f08-4006-b3cb-751762e8bccf",
    type: UserNotification.type.NOTIFICATION,
    title: "Avtalen Oppfølging, tjenesteområde E - Moss utløper om 31.07.2023",
    description: "Beskrivelse",
    user: "B99876",
    createdAt: "2023-01-26T13:51:50.417-07:00",
    readAt: "2023-01-26T13:51:50.417-07:00",
  },
]};
