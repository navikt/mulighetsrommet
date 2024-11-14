import { AvtalerService } from "@mr/api-client";

export async function avtalerLoader() {
  const data = await AvtalerService.getAvtaler();
  return data;
}
