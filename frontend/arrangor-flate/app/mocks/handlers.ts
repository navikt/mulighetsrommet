import { http, HttpResponse } from "msw";
// TODO Sett opp mock-handlers når det trengs
export const handlers = [
  http.get("/hello-world", async () => {
    return HttpResponse.json({ title: "Hello World" });
  }),
];
